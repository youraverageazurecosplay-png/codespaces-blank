package com.example.clientutils.integration;

import com.example.clientutils.ClientUtilsMod;
import net.minecraft.client.MinecraftClient;

public final class LitematicaBridge {
    private LitematicaBridge() {
    }

    public static void openGuide(MinecraftClient client) {
        if (client == null) {
            return;
        }

        if (ClientUtilsMod.isLitematicaLoaded()) {
            ClientUtilsMod.notify(client,
                    "Litematica detected. Use your Litematica keybinds (default M) for schematic browser/build assist.");
            return;
        }

        ClientUtilsMod.notify(client,
                "Litematica is not installed. Add Litematica + MaLiLib to use built-in schematic workflows.");
    }
}
