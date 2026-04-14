package com.example.clientutils.integration;

import com.example.clientutils.ClientUtilsMod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

public final class ReplayModBridge {
    private ReplayModBridge() {
    }

    public static boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded("replaymod");
    }

    public static void openGuide(MinecraftClient client) {
        if (client == null) {
            return;
        }
        if (isLoaded()) {
            ClientUtilsMod.notify(client, "ReplayMod detected and ready.");
        } else {
            ClientUtilsMod.notify(client, "ReplayMod is not installed. Add ReplayMod for recording/replays.");
        }
    }
}

