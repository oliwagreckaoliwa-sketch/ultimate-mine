package autominer.util;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Płynne obracanie kamery gracza (jak zwykły ruch myszą, bez dodatkowych pakietów). */
public final class AimHelper {
    private AimHelper() {}

    public static float yawTo(Vec3d eye, Vec3d p) {
        double dx = p.x - eye.x;
        double dz = p.z - eye.z;
        return (float) (MathHelper.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
    }

    public static float pitchTo(Vec3d eye, Vec3d p) {
        double dx = p.x - eye.x;
        double dy = p.y - eye.y;
        double dz = p.z - eye.z;
        double h = Math.sqrt(dx * dx + dz * dz);
        return (float) -(MathHelper.atan2(dy, h) * 180.0 / Math.PI);
    }

    /**
     * Obraca gracza w stronę punktu o maksymalnie maxStep stopni na wywołanie.
     * @return pozostały błąd kątowy w stopniach
     */
    public static double apply(ClientPlayerEntity player, Vec3d target, double maxStep) {
        Vec3d eye = player.getEyePos();
        float wantYaw = yawTo(eye, target);
        float wantPitch = MathHelper.clamp(pitchTo(eye, target), -90.0f, 90.0f);
        float dYaw = MathHelper.wrapDegrees(wantYaw - player.getYaw());
        float dPitch = wantPitch - player.getPitch();
        double dist = Math.sqrt(dYaw * dYaw + dPitch * dPitch);
        if (dist <= maxStep || dist < 0.0001) {
            player.setYaw(player.getYaw() + dYaw);
            player.setPitch(wantPitch);
            return 0.0;
        }
        double k = maxStep / dist;
        player.setYaw((float) (player.getYaw() + dYaw * k));
        player.setPitch(MathHelper.clamp((float) (player.getPitch() + dPitch * k), -90.0f, 90.0f));
        return dist - maxStep;
    }
}
