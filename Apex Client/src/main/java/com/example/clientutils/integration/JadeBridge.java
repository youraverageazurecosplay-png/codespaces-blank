package com.example.clientutils.integration;

import com.example.clientutils.ClientUtilsMod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

public final class JadeBridge {
    private JadeBridge() {
    }

    public static boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded("jade");
    }

    public static void openGuide(MinecraftClient client) {
        if (client == null) {
            return;
        }
        if (isLoaded()) {
            ClientUtilsMod.notify(client, "Jade detected and ready.");
        } else {
            ClientUtilsMod.notify(client, "Jade is not installed. Add Jade for block/entity HUD overlays.");
        }
    }
}

