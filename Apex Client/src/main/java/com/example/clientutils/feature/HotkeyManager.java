package com.example.clientutils.feature;

import com.example.clientutils.ClientUtilsMod;
import com.example.clientutils.config.ClientUtilsConfig;
import com.example.clientutils.gui.ClientUtilsScreen;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

public class HotkeyManager {
    private boolean prevOpenGui;
    private boolean prevTriggerToggle;
    private boolean prevMacro1;
    private boolean prevMacro2;
    private boolean prevMacro3;
    private boolean prevTargetSelect;
    private boolean prevFreecam;
    private boolean prevXray;
    private boolean prevAutoPot;
    private boolean prevCreativeDump;
    private boolean prevOpenEchestCmd;

    public void handleHotkeys(MinecraftClient client) {
        ClientUtilsConfig config = ClientUtilsMod.CONFIG;

        boolean openGui = isKeyDown(client, config.openGuiKey);
        if (openGui && !prevOpenGui) {
            client.setScreen(new ClientUtilsScreen(client.currentScreen));
        }
        prevOpenGui = openGui;

        boolean toggleTrigger = isKeyDown(client, config.toggleTriggerbotKey);
        if (toggleTrigger && !prevTriggerToggle) {
            config.triggerbotEnabled = !config.triggerbotEnabled;
            config.save();
            ClientUtilsMod.notify(client, "Triggerbot: " + (config.triggerbotEnabled ? "ON" : "OFF"));
        }
        prevTriggerToggle = toggleTrigger;

        boolean selectTarget = isKeyDown(client, config.selectTargetKey);
        if (selectTarget && !prevTargetSelect) {
            ClientUtilsMod.selectPvpTargetUnderCrosshair(client);
        }
        prevTargetSelect = selectTarget;

        boolean freecamToggle = isKeyDown(client, config.freecamKey);
        if (freecamToggle && !prevFreecam) {
            config.freecamEnabled = !config.freecamEnabled;
            if (!config.freecamEnabled) {
                ClientUtilsMod.disableFreecam(client);
            } else {
                ClientUtilsMod.enableFreecam(client);
            }
            config.save();
            ClientUtilsMod.notify(client, "Freecam: " + (config.freecamEnabled ? "ON" : "OFF"));
        }
        prevFreecam = freecamToggle;

        boolean xrayToggle = isKeyDown(client, config.xrayToggleKey);
        if (xrayToggle && !prevXray) {
            config.xrayEnabled = !config.xrayEnabled;
            config.save();
            ClientUtilsMod.notify(client, "Xray: " + (config.xrayEnabled ? "ON" : "OFF"));
        }
        prevXray = xrayToggle;

        boolean autoPot = isKeyDown(client, config.autoPotKey);
        if (autoPot && !prevAutoPot && config.autoPotEnabled) {
            ClientUtilsMod.triggerAutoPot(client);
        }
        prevAutoPot = autoPot;

        boolean creativeDump = isKeyDown(client, config.creativeEchestDumpKey);
        if (creativeDump && !prevCreativeDump) {
            ClientUtilsMod.runCreativeSavedHotbarDump(client);
        }
        prevCreativeDump = creativeDump;

        boolean openEchest = isKeyDown(client, config.openEnderChestCommandKey);
        if (openEchest && !prevOpenEchestCmd && config.enderChestCommandEnabled) {
            ClientUtilsMod.runEnderChestCommand(client);
        }
        prevOpenEchestCmd = openEchest;

        if (config.macroEnabled) {
            boolean m1 = isKeyDown(client, config.macro1Key);
            boolean m2 = isKeyDown(client, config.macro2Key);
            boolean m3 = isKeyDown(client, config.macro3Key);
            if (m1 && !prevMacro1) {
                ClientUtilsMod.MACRO_RUNNER.start(config.macro1);
            }
            if (m2 && !prevMacro2) {
                ClientUtilsMod.MACRO_RUNNER.start(config.macro2);
            }
            if (m3 && !prevMacro3) {
                ClientUtilsMod.MACRO_RUNNER.start(config.macro3);
            }
            prevMacro1 = m1;
            prevMacro2 = m2;
            prevMacro3 = m3;
        }
    }

    private boolean isKeyDown(MinecraftClient client, int key) {
        return key != 0 && (key < 0 ? client.mouse.wasLeftButtonClicked() : GLFW.glfwGetKey(client.getWindow().getHandle(), key) == GLFW.GLFW_PRESS);
    }
}