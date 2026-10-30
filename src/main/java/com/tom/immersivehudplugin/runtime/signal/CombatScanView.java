package com.tom.immersivehudplugin.runtime.signal;

import com.tom.immersivehudplugin.config.GlobalConfig;
import org.joml.Vector3dc;

/** Immutable eye position, look direction and geometry settings shared by one scan. */
record CombatScanView(double originX, double originY, double originZ,
                      double forwardX, double forwardZ, double horizontalLookLength,
                      double lookElevation, double horizontalHalfFov, double verticalHalfFov,
                      double targetHeight, double targetEpsilon) {
    static CombatScanView capture(Vector3dc origin, Vector3dc direction, GlobalConfig config) {
        double horizontalLength = Math.sqrt(direction.x() * direction.x() + direction.z() * direction.z());
        return new CombatScanView(origin.x(), origin.y(), origin.z(),
                horizontalLength > 0 ? direction.x() / horizontalLength : 0,
                horizontalLength > 0 ? direction.z() / horizontalLength : 0,
                horizontalLength, Math.atan2(direction.y(), horizontalLength),
                (config != null ? config.getPlayerCombatHorizontalFovDegrees() : GlobalConfig.PLAYER_COMBAT_HORIZONTAL_FOV_DEGREES) * 0.5d,
                (config != null ? config.getPlayerCombatVerticalFovDegrees() : GlobalConfig.PLAYER_COMBAT_VERTICAL_FOV_DEGREES) * 0.5d,
                config != null ? config.getLosNpcTargetHeight() : GlobalConfig.LOS_NPC_TARGET_HEIGHT,
                config != null ? config.getLosTargetEpsilon() : GlobalConfig.LOS_TARGET_EPSILON);
    }

    boolean insideFov(Vector3dc npcPosition) {
        double dx = npcPosition.x() - originX;
        double dy = npcPosition.y() + targetHeight - originY;
        double dz = npcPosition.z() - originZ;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        if (horizontalDistance > 0.0001d && horizontalLookLength > 0.0001d) {
            double dot = forwardX * (dx / horizontalDistance) + forwardZ * (dz / horizontalDistance);
            double angle = Math.toDegrees(Math.acos(Math.clamp(dot, -1d, 1d)));
            if (!(angle <= horizontalHalfFov)) return false;
        }
        double targetElevation = Math.atan2(dy, horizontalDistance);
        return Math.abs(Math.toDegrees(targetElevation - lookElevation)) <= verticalHalfFov;
    }
}
