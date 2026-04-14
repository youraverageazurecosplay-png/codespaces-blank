package com.example.clientutils.feature;

import com.example.clientutils.ClientUtilsMod;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

public final class MacroRunner {
    private final Queue<String> queue = new ArrayDeque<>();
    private int delayTicks = 0;
    private int stripMineTicks = 0;

    public void start(String macroText) {
        queue.clear();
        delayTicks = 0;
        if (macroText == null || macroText.isBlank()) {
            return;
        }
        Arrays.stream(macroText.split(";"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .forEach(queue::offer);
    }

    public void tick(MinecraftClient client) {
        if (client.player == null || client.getNetworkHandler() == null) {
            return;
        }
        runStripMine(client);
        if (queue.isEmpty()) {
            return;
        }
        if (delayTicks > 0) {
            delayTicks--;
            return;
        }
        String next = queue.poll();
        if (next == null) {
            return;
        }

        if (next.startsWith("#")) {
            runLocalDirective(client, next);
        } else if (next.startsWith("/")) {
            client.getNetworkHandler().sendChatCommand(next.substring(1));
        } else {
            client.getNetworkHandler().sendChatMessage(next);
        }
        delayTicks = 4;
    }

    private void runLocalDirective(MinecraftClient client, String directive) {
        String lower = directive.toLowerCase();
        if (lower.equals("#xray")) {
            ClientUtilsMod.CONFIG.xrayEnabled = !ClientUtilsMod.CONFIG.xrayEnabled;
            ClientUtilsMod.CONFIG.save();
            ClientUtilsMod.notify(client, "Xray " + (ClientUtilsMod.CONFIG.xrayEnabled ? "ON" : "OFF"));
            return;
        }
        if (lower.equals("#xray on")) {
            ClientUtilsMod.CONFIG.xrayEnabled = true;
            ClientUtilsMod.CONFIG.save();
            return;
        }
        if (lower.equals("#xray off")) {
            ClientUtilsMod.CONFIG.xrayEnabled = false;
            ClientUtilsMod.CONFIG.save();
            return;
        }
        if (lower.startsWith("#stripmine")) {
            String[] split = lower.split(" ");
            if (split.length >= 2) {
                try {
                    stripMineTicks = Math.max(0, Integer.parseInt(split[1]));
                    return;
                } catch (NumberFormatException ignored) {
                }
            }
            stripMineTicks = 120;
            return;
        }
        if (lower.startsWith("#waypoint ")) {
            String[] split = directive.split(" ", 2);
            if (split.length == 2) {
                WaypointManager.add(split[1], client.player.getBlockPos());
                ClientUtilsMod.notify(client, "Waypoint added: " + split[1]);
            }
        }
    }

    private void runStripMine(MinecraftClient client) {
        if (stripMineTicks <= 0 || client.currentScreen != null) {
            return;
        }
        if (!ClientUtilsMod.CONFIG.xrayEnabled) {
            client.options.forwardKey.setPressed(true);
            client.options.attackKey.setPressed(true);
        }
        stripMineTicks--;
        if (stripMineTicks == 0) {
            client.options.forwardKey.setPressed(false);
            client.options.attackKey.setPressed(false);
        }
    }

    public boolean isForceWalking() {
        return stripMineTicks > 0;
    }
}
