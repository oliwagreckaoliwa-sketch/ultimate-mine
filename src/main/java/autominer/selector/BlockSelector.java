package autominer.selector;

import autominer.AutoMiner;
import autominer.config.ModConfig;
import autominer.config.ConfigManager;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

/** Odczyt bloku z celownika (F5 / Shift+F5) i rysowanie obrysu zaznaczenia. */
public final class BlockSelector {

    public static BlockHitResult targetedBlock(MinecraftClient c) {
        if (c.world == null || c.player == null) return null;
        HitResult h = c.crosshairTarget;
        if (h instanceof BlockHitResult bhr && h.getType() == HitResult.Type.BLOCK) {
            return bhr;
        }
        return null;
    }

    /** Obsluga skrotu: dodaje blok, a z Shiftem usuwa. W trybie wyboru skrzynki wybiera skrzynke. */
    public void handleKey(MinecraftClient c, boolean shift) {
        if (c.world == null || c.player == null) return;
        if (AutoMiner.chests.isPicking()) {
            AutoMiner.chests.pickFromCrosshair(c);
            return;
        }
        BlockHitResult hit = targetedBlock(c);
        if (hit == null) {
            AutoMiner.notifications.warn("NO BLOCK SELECTED");
            return;
        }
        String id = BlockInfo.idOf(c.world.getBlockState(hit.getBlockPos()).getBlock());
        String name = BlockInfo.name(id);
        BlockSelectionManager sel = AutoMiner.selection;
        if (shift) {
            switch (sel.remove(id)) {
                case REMOVED -> AutoMiner.notifications.info("BLOCK REMOVED: " + name);
                case NOT_SELECTED -> AutoMiner.notifications.warn("BLOCK NOT SELECTED: " + name);
                default -> AutoMiner.notifications.error("INVALID BLOCK");
            }
        } else {
            switch (sel.add(id)) {
                case ADDED -> AutoMiner.notifications.success("BLOCK ADDED: " + name);
                case ALREADY -> AutoMiner.notifications.warn("BLOCK ALREADY SELECTED");
                default -> AutoMiner.notifications.error("INVALID BLOCK");
            }
        }
    }

    /** Rejestruje rysowanie obrysu wskazywanego bloku (zielony = dodany, szary = nie). */
    public void registerRender() {
        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            MinecraftClient c = MinecraftClient.getInstance();
            if (c.world == null || c.player == null || c.currentScreen != null) return;
            ModConfig cfg = ConfigManager.get();
            VertexConsumerProvider consumers = ctx.consumers();
            MatrixStack ms = ctx.matrixStack();
            if (consumers == null || ms == null) return;
            Vec3d cam = ctx.camera().getPos();
            VertexConsumer vc = consumers.getBuffer(RenderLayer.getLines());
            boolean drew = false;

            if (cfg.showOutline) {
                BlockHitResult hit = targetedBlock(c);
                if (hit != null) {
                    BlockPos pos = hit.getBlockPos();
                    String id = BlockInfo.idOf(c.world.getBlockState(pos).getBlock());
                    boolean sel = AutoMiner.selection.isSelected(id);
                    drew |= outline(c, ms, vc, cam, pos, sel ? 0.13f : 0.62f, sel ? 0.77f : 0.62f, sel ? 0.37f : 0.65f);
                }
            }
            BlockPos chest = AutoMiner.chests.selectedPosInCurrentWorld(c);
            if (chest != null) {
                drew |= outline(c, ms, vc, cam, chest, 0.55f, 0.36f, 0.96f);
            }
            if (drew && consumers instanceof VertexConsumerProvider.Immediate imm) {
                imm.draw(RenderLayer.getLines());
            }
        });
    }

    private static boolean outline(MinecraftClient c, MatrixStack ms, VertexConsumer vc, Vec3d cam,
                                   BlockPos pos, float r, float g, float b) {
        BlockState st = c.world.getBlockState(pos);
        if (st.isAir()) return false;
        VoxelShape shape = st.getOutlineShape(c.world, pos, ShapeContext.of(c.player));
        if (shape.isEmpty()) return false;
        Box box = shape.getBoundingBox().offset(pos).expand(0.003).offset(-cam.x, -cam.y, -cam.z);
        WorldRenderer.drawBox(ms, vc, box, r, g, b, 1.0f);
        return true;
    }
}
