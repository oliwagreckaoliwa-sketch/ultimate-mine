package autominer.mining;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/** Wyszukiwanie najblizszego zaznaczonego bloku w zasiegu gracza. */
public final class MiningTargetSelector {
    private static final int MAX_RAYCASTS = 12;

    private MiningTargetSelector() {}

    public record Target(BlockPos pos, Vec3d aim, double distSq) {}

    private record Candidate(BlockPos pos, double distSq) {}

    private static List<Candidate> candidates(MinecraftClient c, BlockWhitelist wl, double range, Predicate<BlockPos> skip) {
        ClientPlayerEntity p = c.player;
        ClientWorld w = c.world;
        Vec3d eye = p.getEyePos();
        BlockPos base = p.getBlockPos();
        int r = (int) Math.ceil(range) + 1;
        double max = range * range;
        List<Candidate> out = new ArrayList<>();
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    m.set(base.getX() + dx, base.getY() + dy, base.getZ() + dz);
                    double d = eye.squaredDistanceTo(Vec3d.ofCenter(m));
                    if (d > max) continue;
                    BlockState st = w.getBlockState(m);
                    if (st.isAir() || !wl.contains(st.getBlock())) continue;
                    if (st.getHardness(w, m) < 0) continue;
                    BlockPos imm = m.toImmutable();
                    if (skip.test(imm)) continue;
                    out.add(new Candidate(imm, d));
                }
            }
        }
        out.sort(Comparator.comparingDouble(Candidate::distSq));
        return out;
    }

    /** Najblizszy blok, ktory da sie trafic celownikiem w zasiegu. */
    public static Target find(MinecraftClient c, BlockWhitelist wl, double range, boolean visibleOnly, Predicate<BlockPos> skip) {
        List<Candidate> list = candidates(c, wl, range, skip);
        Vec3d eye = c.player.getEyePos();
        int tested = 0;
        for (Candidate cand : list) {
            Vec3d center = Vec3d.ofCenter(cand.pos());
            if (!visibleOnly) {
                return new Target(cand.pos(), center, cand.distSq());
            }
            if (tested++ >= MAX_RAYCASTS) break;
            BlockHitResult hr = c.world.raycast(new RaycastContext(eye, center,
                    RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, c.player));
            if (hr.getType() == HitResult.Type.BLOCK && hr.getBlockPos().equals(cand.pos())
                    && eye.distanceTo(hr.getPos()) <= range) {
                Vec3d inward = center.subtract(hr.getPos());
                Vec3d aim = inward.lengthSquared() > 1.0E-6
                        ? hr.getPos().add(inward.normalize().multiply(0.05)) : hr.getPos();
                return new Target(cand.pos(), aim, cand.distSq());
            }
        }
        return null;
    }

    /** Najblizszy zaznaczony blok bez sprawdzania widocznosci (do dojscia). */
    public static BlockPos findNearest(MinecraftClient c, BlockWhitelist wl, double range, Predicate<BlockPos> skip) {
        List<Candidate> list = candidates(c, wl, range, skip);
        return list.isEmpty() ? null : list.get(0).pos();
    }
}
