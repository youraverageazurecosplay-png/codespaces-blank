package com.example.clientutils.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

public final class ClientUtilsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("clientutils.json");

    public boolean triggerbotEnabled = false;
    public boolean autoSprintEnabled = true;
    public boolean fullBrightEnabled = false;
    public boolean autoWalkEnabled = false;
    public boolean allowFormattingCodes = true;
    public boolean allowBookClipboardImport = true;
    public boolean autoAttributeSwapEnabled = false;
    public String attributeSwapSlots = "1,2,3";
    public boolean autoBookbanWriterEnabled = false;
    public boolean autoTotemRefillEnabled = true;
    public boolean autoToolSwapEnabled = true;
    public boolean autoMainHandRefillEnabled = true;
    public boolean fireworkWithoutHoldingEnabled = true;
    public boolean middleClickFriendsEnabled = true;
    public boolean antiAfkEnabled = false;
    public boolean macroEnabled = true;
    public boolean freecamEnabled = false;
    public boolean autoCrystalEnabled = false;
    public boolean autoAnchorEnabled = false;
    public boolean advancedAutoPvpEnabled = false;
    public boolean xrayEnabled = false;
    public boolean espEnabled = false;
    public boolean blockEspEnabled = false;
    public boolean storageEspEnabled = false;
    public boolean tracersEnabled = false;
    public boolean waypointEnabled = true;
    public boolean clearGlassEnabled = true;
    public boolean smoothAimEnabled = true;
    public boolean stopWhenTargetDeadEnabled = true;
    public boolean projectileTrajectoryEnabled = true;
    public boolean voidPearlSafetyEnabled = true;
    public boolean trapDetectionEnabled = true;
    public boolean autoPotEnabled = true;
    public boolean autoEatEnabled = true;
    public boolean privateMessageCaptureEnabled = true;
    public boolean autoRespawnEnabled = true;
    public boolean autoRejoinEnabled = false;
    public boolean autoLeaveLowHealthEnabled = false;
    public float lowHealthLeaveThreshold = 6.0f;
    public boolean enderChestCommandEnabled = true;
    public int autoEatHungerThreshold = 12;
    public int combatRange = 6;
    public int espRange = 24;
    public int autoRejoinDelaySeconds = 5;

    public int openGuiKey = 344;
    public int toggleTriggerbotKey = 96;
    public int macro1Key = 82;
    public int macro2Key = 79;
    public int macro3Key = 80;
    public int bookbanFillKey = 66;
    public int selectTargetKey = 86;
    public int freecamKey = 70;
    public int xrayToggleKey = 88;
    public int autoPotKey = 72;
    public int creativeEchestDumpKey = 74;
    public int openEnderChestCommandKey = 75;
    public int creativeSavedHotbarIndex = 0;
    public int creativeSavedHotbarSlot = 0;

    public String macro1 = "#goto x z";
    public String macro2 = "/home";
    public String macro3 = "/spawn";
    public String autoPotPriority = "healing,strength,swiftness,regeneration";
    public String enderChestCommand = "/echest";

    public Set<String> friends = new LinkedHashSet<>();
    public Set<String> waypoints = new LinkedHashSet<>();

    public static ClientUtilsConfig load() {
        if (!Files.exists(CONFIG_PATH)) {
            ClientUtilsConfig config = new ClientUtilsConfig();
            config.save();
            return config;
        }

        try {
            String json = Files.readString(CONFIG_PATH);
            ClientUtilsConfig config = GSON.fromJson(json, ClientUtilsConfig.class);
            if (config == null) {
                return new ClientUtilsConfig();
            }
            if (config.friends == null) {
                config.friends = new LinkedHashSet<>();
            }
            if (config.waypoints == null) {
                config.waypoints = new LinkedHashSet<>();
            }
            return config;
        } catch (Exception e) {
            ClientUtilsConfig config = new ClientUtilsConfig();
            config.save();
            return config;
        }
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (IOException ignored) {
        }
    }
}
