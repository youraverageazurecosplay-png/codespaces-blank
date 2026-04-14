package com.example.clientutils.feature;

import net.minecraft.entity.player.PlayerEntity;

import java.util.UUID;

public final class PvpTargetManager {
    private static UUID targetUuid;
    private static String targetName = "";

    private PvpTargetManager() {
    }

    public static void select(PlayerEntity player) {
        if (player == null) {
            return;
        }
        targetUuid = player.getUuid();
        targetName = player.getName().getString();
    }

    public static UUID getTargetUuid() {
        return targetUuid;
    }

    public static String getTargetName() {
        return targetName;
    }

    public static void clear() {
        targetUuid = null;
        targetName = "";
    }
}

