package autominer.mining;

import autominer.AutoMiner;
import autominer.config.ConfigManager;
import autominer.config.ModConfig;
import autominer.selector.BlockInfo;
import autominer.util.AimHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Steruje pojedynczym celem: celuje kamera, wybiera kilof i "trzyma" klawisz ataku
 * (standardowa operacja klienta). Nie wysyla wlasnych pakietow.
 */
public final class MiningController {
    public enum Outcome { MINING, SEARCHING, WALKING, STOP_NO_TOOL, STOP_SERVER, STOP_IDLE }

    private record Pending(BlockPos pos, Block block, long deadline) {}

    private static final int CONFIRM_TICKS = 6;
    private static final int BLOCK_TIMEOUT_TICKS = 20 * 25;
    private static final int MAX_WALK_TICKS = 400;

    private final BlockWhitelist whitelist;
    private final Map<BlockPos, Long> skip = new HashMap<>();
    private final List<Pending> pending = new ArrayList<>();

    private BlockPos target;
    private Block targetBlock;
    private Vec3d aim;
    private long targetMiningSince = -1;
    private long lastScan = -1000;
    private long noTargetSince = -1;
    private int walkTicks;
    private int rejections;
    private int timeouts;
    private boolean attackHeld;
    private boolean forwardHeld;
    private boolean jumpHeld;
    private boolean serverBlocking;

    /** (identyfikator bloku, tick) po potwierdzonym wykopaniu. */
    public BiConsumer<String, Long> onConfirmed = (id, t) -> {};

    public MiningController(BlockWhitelist whitelist) {
        this.whitelist = whitelist;
    }

    public BlockPos target() {
        return target;
    }

    public boolean serverBlocking() {
        return serverBlocking;
    }

    public void reset(MinecraftClient c) {
        releaseKeys(c);
        target = null;
        targetBlock = null;
        aim = null;
        targetMiningSince = -1;
        noTargetSince = -1;
        walkTicks = 0;
        rejections = 0;
        timeouts = 0;
        serverBlocking = false;
        skip.clear();
    }

    public void releaseKeys(MinecraftClient c) {
        if (attackHeld) {
            c.options.attackKey.setPressed(false);
            attackHeld = false;
        }
        releaseMovement(c);
    }

    private void releaseMovement(MinecraftClient c) {
        if (forwardHeld) {
            c.options.forwardKey.setPressed(false);
            forwardHeld = false;
        }
        if (jumpHeld) {
            c.options.jumpKey.setPressed(false);
            jumpHeld = false;
        }
    }

    /** Potwierdza wykopanie dopiero po kilku tickach, gdy serwer nie cofnal zmiany bloku. */
    public void processPending(MinecraftClient c, long tick) {
        if (c.world == null || pending.isEmpty()) return;
        Iterator<Pending> it = pending.iterator();
        while (it.hasNext()) {
            Pending pd = it.next();
            if (tick < pd.deadline()) continue;
            it.remove();
            Block now = c.world.getBlockState(pd.pos()).getBlock();
            if (now == pd.block()) {
                rejections++;
                skip.put(pd.pos(), tick + 600);
                if (rejections >= 6) serverBlocking = true;
            } else {
                rejections = 0;
                timeouts = 0;
                onConfirmed.accept(BlockInfo.idOf(pd.block()), tick);
            }
        }
    }

    public Outcome tick(MinecraftClient c, long tick) {
        ModConfig cfg = ConfigManager.get();
        ClientPlayerEntity p = c.player;
        double reach = Math.min(cfg.miningRange, p.getBlockInteractionRange() - 0.2);

        if (skip.size() > 256) skip.values().removeIf(u -> u <= tick);

        // 1) wykrycie wykopania celu
        if (target != null) {
            BlockState st = c.world.getBlockState(target);
            if (st.isAir() || st.getBlock() != targetBlock) {
                if (attackHeld && targetMiningSince >= 0) {
                    pending.add(new Pending(target, targetBlock, tick + CONFIRM_TICKS));
                }
                clearTarget();
            } else if (!whitelist.contains(st.getBlock())
                    || p.getEyePos().distanceTo(Vec3d.ofCenter(target)) > reach + 0.9
                    || isSkipped(target, tick)) {
                clearTarget();
            }
        }

        // 2) nowy cel
        if (target == null && tick - lastScan >= cfg.scanIntervalTicks) {
            lastScan = tick;
            MiningTargetSelector.Target t = MiningTargetSelector.find(c, whitelist, reach, cfg.visibleOnly, pos -> isSkipped(pos, tick));
            if (t != null) {
                target = t.pos();
                targetBlock = c.world.getBlockState(target).getBlock();
                aim = t.aim();
                targetMiningSince = -1;
                walkTicks = 0;
            }
        }

        if (target == null) {
            if (attackHeld) {
                c.options.attackKey.setPressed(false);
                attackHeld = false;
            }
            if (noTargetSince < 0) noTargetSince = tick;
            if (cfg.idleStopSeconds > 0 && tick - noTargetSince > cfg.idleStopSeconds * 20L) {
                return Outcome.STOP_IDLE;
            }
            if (cfg.walkToTargets && walkTicks < MAX_WALK_TICKS) {
                BlockPos near = MiningTargetSelector.findNearest(c, whitelist, cfg.walkRange, pos -> isSkipped(pos, tick));
                if (near != null && walk(c, near)) {
                    walkTicks++;
                    return Outcome.WALKING;
                }
                if (near != null && walkTicks >= MAX_WALK_TICKS) {
                    skip.put(near, tick + 1200);
                }
            } else if (cfg.walkToTargets) {
                walkTicks = 0;
            }
            releaseMovement(c);
            return Outcome.SEARCHING;
        }
        noTargetSince = -1;
        releaseMovement(c);

        // 3) narzedzie
        BlockState state = c.world.getBlockState(target);
        int slot = ToolManager.bestSlot(p, state, cfg.minDurability);
        if (slot < 0 && cfg.requirePickaxe) {
            releaseKeys(c);
            return Outcome.STOP_NO_TOOL;
        }
        if (slot >= 0 && p.getInventory().selectedSlot != slot) {
            p.getInventory().selectedSlot = slot;
        }

        // 4) celowanie i kopanie (tylko zaznaczone bloki)
        double err = AimHelper.apply(p, aim, cfg.rotationSpeed);
        boolean onTarget = false;
        HitResult h = c.crosshairTarget;
        if (h instanceof BlockHitResult bhr && h.getType() == HitResult.Type.BLOCK) {
            BlockPos hp = bhr.getBlockPos();
            if (hp.equals(target)) {
                onTarget = true;
            } else {
                Block hb = c.world.getBlockState(hp).getBlock();
                if (whitelist.contains(hb) && !isSkipped(hp, tick)) {
                    target = hp;
                    targetBlock = hb;
                    targetMiningSince = -1;
                    onTarget = true;
                }
            }
        }
        if (onTarget && err < 6.0) {
            if (!attackHeld) {
                c.options.attackKey.setPressed(true);
                attackHeld = true;
            }
            if (targetMiningSince < 0) targetMiningSince = tick;
            if (tick - targetMiningSince > BLOCK_TIMEOUT_TICKS) {
                skip.put(target, tick + 1200);
                timeouts++;
                clearTarget();
                if (timeouts >= 3) serverBlocking = true;
            }
        } else if (attackHeld) {
            c.options.attackKey.setPressed(false);
            attackHeld = false;
            targetMiningSince = -1;
        }

        if (serverBlocking) {
            releaseKeys(c);
            return Outcome.STOP_SERVER;
        }
        return Outcome.MINING;
    }

    private void clearTarget() {
        target = null;
        targetBlock = null;
        aim = null;
        targetMiningSince = -1;
    }

    private boolean isSkipped(BlockPos pos, long tick) {
        Long until = skip.get(pos);
        return until != null && until > tick;
    }

    /** Idzie w strone bloku. Zwraca false, gdy ruch jest niebezpieczny. */
    private boolean walk(MinecraftClient c, BlockPos to) {
        ClientPlayerEntity p = c.player;
        AimHelper.apply(p, Vec3d.ofCenter(to), ConfigManager.get().rotationSpeed);
        if (!safeAhead(c)) {
            releaseMovement(c);
            return false;
        }
        if (!forwardHeld) {
            c.options.forwardKey.setPressed(true);
            forwardHeld = true;
        }
        boolean wantJump = p.horizontalCollision && p.isOnGround();
        if (wantJump != jumpHeld) {
            c.options.jumpKey.setPressed(wantJump);
            jumpHeld = wantJump;
        }
        return true;
    }

    private boolean safeAhead(MinecraftClient c) {
        ClientPlayerEntity p = c.player;
        Vec3d look = Vec3d.fromPolar(0.0f, p.getYaw());
        BlockPos ahead = p.getBlockPos().add((int) Math.round(look.x), 0, (int) Math.round(look.z));
        for (BlockPos q : new BlockPos[] {ahead, ahead.up(), ahead.down()}) {
            BlockState st = c.world.getBlockState(q);
            if (st.getFluidState().isIn(FluidTags.LAVA)) return false;
            if (st.isOf(Blocks.FIRE) || st.isOf(Blocks.SOUL_FIRE) || st.isOf(Blocks.MAGMA_BLOCK)
                    || st.isOf(Blocks.CACTUS) || st.isOf(Blocks.SWEET_BERRY_BUSH)) {
                return false;
            }
        }
        for (int k = 1; k <= 3; k++) {
            if (!c.world.getBlockState(ahead.down(k)).isAir()) return true;
        }
        return false; // przepasc glebsza niz 3 bloki
    }
}
