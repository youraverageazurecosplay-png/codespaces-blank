package com.example.clientutils.gui;

import com.example.clientutils.ClientUtilsMod;
import com.example.clientutils.feature.FriendManager;
import com.example.clientutils.feature.WaypointManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ClientUtilsScreen extends Screen {
    private final Screen parent;
    private Page page = Page.MODULES;
    private TextFieldWidget friendInput;
    private TextFieldWidget macro1Field;
    private TextFieldWidget macro2Field;
    private TextFieldWidget macro3Field;
    private int waitingForHotkey = 0;

    public ClientUtilsScreen(Screen parent) {
        super(Text.translatable("screen.clientutils.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearChildren();
        int centerX = this.width / 2;
        int top = 36;

        addDrawableChild(ButtonWidget.builder(Text.literal("General"), b -> switchPage(Page.MODULES))
                .dimensions(centerX - 180, 8, 70, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Utility"), b -> switchPage(Page.UTILITY))
                .dimensions(centerX - 255, 8, 70, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Combat"), b -> switchPage(Page.COMBAT))
                .dimensions(centerX - 105, 8, 70, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Render"), b -> switchPage(Page.RENDER))
                .dimensions(centerX - 30, 8, 70, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Friends"), b -> switchPage(Page.FRIENDS))
                .dimensions(centerX + 45, 8, 70, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Macros"), b -> switchPage(Page.MACROS))
                .dimensions(centerX + 120, 8, 70, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Hotkeys"), b -> switchPage(Page.HOTKEYS))
                .dimensions(centerX - 60, this.height - 28, 120, 20).build());

        if (page == Page.MODULES) {
            initModules(centerX, top);
        } else if (page == Page.UTILITY) {
            initUtility(centerX, top);
        } else if (page == Page.COMBAT) {
            initCombat(centerX, top);
        } else if (page == Page.RENDER) {
            initRender(centerX, top);
        } else if (page == Page.FRIENDS) {
            initFriends(centerX, top);
        } else if (page == Page.MACROS) {
            initMacros(centerX, top);
        } else if (page == Page.HOTKEYS) {
            initHotkeys(centerX, top);
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> close())
                .dimensions(centerX + 70, this.height - 28, 120, 20)
                .build());
    }

    private void initModules(int centerX, int row) {
        addDrawableChild(toggleButton(centerX, row, "Auto Sprint", () -> ClientUtilsMod.CONFIG.autoSprintEnabled,
                value -> ClientUtilsMod.CONFIG.autoSprintEnabled = value));
        row += 22;
        addDrawableChild(toggleButton(centerX, row, "Full Bright", () -> ClientUtilsMod.CONFIG.fullBrightEnabled,
                value -> ClientUtilsMod.CONFIG.fullBrightEnabled = value));
        row += 22;
        addDrawableChild(toggleButton(centerX, row, "Auto Walk", () -> ClientUtilsMod.CONFIG.autoWalkEnabled,
                value -> ClientUtilsMod.CONFIG.autoWalkEnabled = value));
        row += 22;
        addDrawableChild(toggleButton(centerX, row, "Auto Totem Refill", () -> ClientUtilsMod.CONFIG.autoTotemRefillEnabled,
                value -> ClientUtilsMod.CONFIG.autoTotemRefillEnabled = value));
        row += 22;
        addDrawableChild(toggleButton(centerX, row, "Auto Tool Swap", () -> ClientUtilsMod.CONFIG.autoToolSwapEnabled,
                value -> ClientUtilsMod.CONFIG.autoToolSwapEnabled = value));
        row += 22;
        addDrawableChild(toggleButton(centerX, row, "Auto Main-Hand Refill", () -> ClientUtilsMod.CONFIG.autoMainHandRefillEnabled,
                value -> ClientUtilsMod.CONFIG.autoMainHandRefillEnabled = value));
        row += 22;
        addDrawableChild(toggleButton(centerX, row, "Auto Attribute Swap", () -> ClientUtilsMod.CONFIG.autoAttributeSwapEnabled,
                value -> ClientUtilsMod.CONFIG.autoAttributeSwapEnabled = value));
        row += 22;
        addDrawableChild(toggleButton(centerX, row, "Firework No-Hold", () -> ClientUtilsMod.CONFIG.fireworkWithoutHoldingEnabled,
                value -> ClientUtilsMod.CONFIG.fireworkWithoutHoldingEnabled = value));
        row += 22;
        addDrawableChild(toggleButton(centerX, row, "Formatting Codes", () -> ClientUtilsMod.CONFIG.allowFormattingCodes,
                value -> ClientUtilsMod.CONFIG.allowFormattingCodes = value));
        row += 22;
        addDrawableChild(toggleButton(centerX, row, "Book Clipboard Import", () -> ClientUtilsMod.CONFIG.allowBookClipboardImport,
                value -> ClientUtilsMod.CONFIG.allowBookClipboardImport = value));
        row += 22;
        addDrawableChild(toggleButton(centerX, row, "Bookban Writer", () -> ClientUtilsMod.CONFIG.autoBookbanWriterEnabled,
                value -> ClientUtilsMod.CONFIG.autoBookbanWriterEnabled = value));
        row += 24;
        addDrawableChild(toggleButton(centerX, row, "Projectile Prediction", () -> ClientUtilsMod.CONFIG.projectileTrajectoryEnabled,
                value -> ClientUtilsMod.CONFIG.projectileTrajectoryEnabled = value));
        row += 22;
        addDrawableChild(toggleButton(centerX, row, "Void Pearl Safety", () -> ClientUtilsMod.CONFIG.voidPearlSafetyEnabled,
                value -> ClientUtilsMod.CONFIG.voidPearlSafetyEnabled = value));
        row += 22;
        addDrawableChild(toggleButton(centerX, row, "Trap Detection", () -> ClientUtilsMod.CONFIG.trapDetectionEnabled,
                value -> ClientUtilsMod.CONFIG.trapDetectionEnabled = value));
        row += 22;
        addDrawableChild(toggleButton(centerX, row, "Auto Pot", () -> ClientUtilsMod.CONFIG.autoPotEnabled,
                value -> ClientUtilsMod.CONFIG.autoPotEnabled = value));
        row += 22;
        addDrawableChild(toggleButton(centerX, row, "Auto Eat", () -> ClientUtilsMod.CONFIG.autoEatEnabled,
                value -> ClientUtilsMod.CONFIG.autoEatEnabled = value));
        row += 24;
    }

    private void initUtility(int centerX, int row) {
        addDrawableChild(toggleButton(centerX, row, "PM Capture/Log", () -> ClientUtilsMod.CONFIG.privateMessageCaptureEnabled,
                value -> ClientUtilsMod.CONFIG.privateMessageCaptureEnabled = value));
        row += 22;
        addDrawableChild(toggleButton(centerX, row, "Auto Respawn", () -> ClientUtilsMod.CONFIG.autoRespawnEnabled,
                value -> ClientUtilsMod.CONFIG.autoRespawnEnabled = value));
        row += 22;
        addDrawableChild(toggleButton(centerX, row, "Auto Rejoin", () -> ClientUtilsMod.CONFIG.autoRejoinEnabled,
                value -> ClientUtilsMod.CONFIG.autoRejoinEnabled = value));
        row += 22;
        addDrawableChild(toggleButton(centerX, row, "Auto Leave Low HP", () -> ClientUtilsMod.CONFIG.autoLeaveLowHealthEnabled,
                value -> ClientUtilsMod.CONFIG.autoLeaveLowHealthEnabled = value));
        row += 22;
        addDrawableChild(toggleButton(centerX, row, "EChest Command", () -> ClientUtilsMod.CONFIG.enderChestCommandEnabled,
                value -> ClientUtilsMod.CONFIG.enderChestCommandEnabled = value));
        row += 24;
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Saved Hotbar Row: " + (ClientUtilsMod.CONFIG.creativeSavedHotbarIndex + 1)),
                b -> {
                    ClientUtilsMod.CONFIG.creativeSavedHotbarIndex = (ClientUtilsMod.CONFIG.creativeSavedHotbarIndex + 1) % 9;
                    ClientUtilsMod.CONFIG.save();
                    init();
                }).dimensions(centerX - 100, row, 200, 20).build());
        row += 22;
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Saved Hotbar Slot: " + (ClientUtilsMod.CONFIG.creativeSavedHotbarSlot + 1)),
                b -> {
                    ClientUtilsMod.CONFIG.creativeSavedHotbarSlot = (ClientUtilsMod.CONFIG.creativeSavedHotbarSlot + 1) % 9;
                    ClientUtilsMod.CONFIG.save();
                    init();
                }).dimensions(centerX - 100, row, 200, 20).build());
        row += 24;

        addDrawableChild(ButtonWidget.builder(
                Text.literal(ClientUtilsMod.isLitematicaLoaded() ? "Litematica: Loaded" : "Litematica: Not Detected"),
                button -> ClientUtilsMod.openLitematicaGuide(this.client)
        ).dimensions(centerX - 100, row, 200, 20).build());
        row += 22;

        addDrawableChild(ButtonWidget.builder(
                Text.literal(ClientUtilsMod.isJadeLoaded() ? "Jade: Loaded" : "Jade: Not Detected"),
                button -> ClientUtilsMod.openJadeGuide(this.client)
        ).dimensions(centerX - 100, row, 200, 20).build());
        row += 22;
        addDrawableChild(ButtonWidget.builder(
                Text.literal(ClientUtilsMod.isReplayModLoaded() ? "ReplayMod: Loaded" : "ReplayMod: Not Detected"),
                button -> ClientUtilsMod.openReplayModGuide(this.client)
        ).dimensions(centerX - 100, row, 200, 20).build());
    }

    private void initCombat(int centerX, int row) {
        addDrawableChild(toggleButton(centerX, row, "Triggerbot", () -> ClientUtilsMod.CONFIG.triggerbotEnabled,
                value -> ClientUtilsMod.CONFIG.triggerbotEnabled = value));
        row += 22;
        addDrawableChild(toggleButton(centerX, row, "Auto Attribute Swap", () -> ClientUtilsMod.CONFIG.autoAttributeSwapEnabled,
                value -> ClientUtilsMod.CONFIG.autoAttributeSwapEnabled = value));
        row += 22;
        addDrawableChild(toggleButton(centerX, row, "Auto Crystal", () -> ClientUtilsMod.CONFIG.autoCrystalEnabled,
                value -> ClientUtilsMod.CONFIG.autoCrystalEnabled = value));
        row += 22;
        addDrawableChild(toggleButton(centerX, row, "Auto Anchor", () -> ClientUtilsMod.CONFIG.autoAnchorEnabled,
                value -> ClientUtilsMod.CONFIG.autoAnchorEnabled = value));
        row += 22;
        addDrawableChild(toggleButton(centerX, row, "Advanced Auto PvP", () -> ClientUtilsMod.CONFIG.advancedAutoPvpEnabled,
                value -> ClientUtilsMod.CONFIG.advancedAutoPvpEnabled = value));
        row += 22;
        addDrawableChild(toggleButton(centerX, row, "Smooth Aim", () -> ClientUtilsMod.CONFIG.smoothAimEnabled,
                value -> ClientUtilsMod.CONFIG.smoothAimEnabled = value));
        row += 22;
        addDrawableChild(toggleButton(centerX, row, "Stop On Target Death", () -> ClientUtilsMod.CONFIG.stopWhenTargetDeadEnabled,
                value -> ClientUtilsMod.CONFIG.stopWhenTargetDeadEnabled = value));
    }

    private void initRender(int centerX, int row) {
        addDrawableChild(toggleButton(centerX, row, "Freecam", () -> ClientUtilsMod.CONFIG.freecamEnabled,
                value -> ClientUtilsMod.CONFIG.freecamEnabled = value));
        row += 22;
        addDrawableChild(toggleButton(centerX, row, "Xray", () -> ClientUtilsMod.CONFIG.xrayEnabled,
                value -> ClientUtilsMod.CONFIG.xrayEnabled = value));
        row += 22;
        addDrawableChild(toggleButton(centerX, row, "ESP", () -> ClientUtilsMod.CONFIG.espEnabled,
                value -> ClientUtilsMod.CONFIG.espEnabled = value));
        row += 22;
        addDrawableChild(toggleButton(centerX, row, "Block ESP", () -> ClientUtilsMod.CONFIG.blockEspEnabled,
                value -> ClientUtilsMod.CONFIG.blockEspEnabled = value));
        row += 22;
        addDrawableChild(toggleButton(centerX, row, "Storage ESP", () -> ClientUtilsMod.CONFIG.storageEspEnabled,
                value -> ClientUtilsMod.CONFIG.storageEspEnabled = value));
        row += 22;
        addDrawableChild(toggleButton(centerX, row, "Tracers", () -> ClientUtilsMod.CONFIG.tracersEnabled,
                value -> ClientUtilsMod.CONFIG.tracersEnabled = value));
        row += 22;
        addDrawableChild(toggleButton(centerX, row, "Waypoints", () -> ClientUtilsMod.CONFIG.waypointEnabled,
                value -> ClientUtilsMod.CONFIG.waypointEnabled = value));
        row += 22;
        addDrawableChild(toggleButton(centerX, row, "Clear Glass", () -> ClientUtilsMod.CONFIG.clearGlassEnabled,
                value -> ClientUtilsMod.CONFIG.clearGlassEnabled = value));
        row += 26;

        addDrawableChild(ButtonWidget.builder(Text.literal("Add Waypoint Here"), b -> {
            if (this.client == null || this.client.player == null) {
                return;
            }
            String name = "wp_" + (System.currentTimeMillis() % 100000);
            WaypointManager.add(name, this.client.player.getBlockPos());
        }).dimensions(centerX - 100, row, 200, 20).build());
        row += 22;

        addDrawableChild(ButtonWidget.builder(Text.literal("Remove Nearest Waypoint"), b -> {
            if (this.client == null || this.client.player == null) {
                return;
            }
            WaypointManager.Waypoint nearest = WaypointManager.nearest(
                    this.client.player.getX(), this.client.player.getY(), this.client.player.getZ());
            if (nearest != null) {
                WaypointManager.remove(nearest.name());
            }
        }).dimensions(centerX - 100, row, 200, 20).build());
    }

    private void initFriends(int centerX, int row) {
        friendInput = new TextFieldWidget(this.textRenderer, centerX - 100, row, 140, 20, Text.literal("Friend"));
        friendInput.setPlaceholder(Text.literal("Player name"));
        addDrawableChild(friendInput);

        addDrawableChild(ButtonWidget.builder(Text.literal("Add"), b -> {
            FriendManager.add(friendInput.getText());
            friendInput.setText("");
            init();
        }).dimensions(centerX + 44, row, 56, 20).build());
        row += 28;

        List<String> friends = new ArrayList<>(ClientUtilsMod.CONFIG.friends);
        friends.sort(Comparator.naturalOrder());
        int shown = Math.min(8, friends.size());
        for (int i = 0; i < shown; i++) {
            String name = friends.get(i);
            int y = row + i * 22;
            addDrawableChild(ButtonWidget.builder(Text.literal("Remove " + name), b -> {
                FriendManager.remove(name);
                init();
            }).dimensions(centerX - 100, y, 200, 20).build());
        }
    }

    private void initMacros(int centerX, int row) {
        macro1Field = addMacroField(centerX, row, ClientUtilsMod.CONFIG.macro1, text -> ClientUtilsMod.CONFIG.macro1 = text);
        row += 24;
        macro2Field = addMacroField(centerX, row, ClientUtilsMod.CONFIG.macro2, text -> ClientUtilsMod.CONFIG.macro2 = text);
        row += 24;
        macro3Field = addMacroField(centerX, row, ClientUtilsMod.CONFIG.macro3, text -> ClientUtilsMod.CONFIG.macro3 = text);
        row += 26;

        addDrawableChild(toggleButton(centerX, row, "Macros Enabled", () -> ClientUtilsMod.CONFIG.macroEnabled,
                value -> ClientUtilsMod.CONFIG.macroEnabled = value));
        row += 24;

        addDrawableChild(ButtonWidget.builder(Text.literal("Save Macros"), b -> {
            ClientUtilsMod.CONFIG.macro1 = macro1Field.getText();
            ClientUtilsMod.CONFIG.macro2 = macro2Field.getText();
            ClientUtilsMod.CONFIG.macro3 = macro3Field.getText();
            ClientUtilsMod.CONFIG.save();
        }).dimensions(centerX - 100, row, 200, 20).build());
    }

    private TextFieldWidget addMacroField(int centerX, int y, String value, MacroSetter setter) {
        TextFieldWidget field = new TextFieldWidget(this.textRenderer, centerX - 100, y, 200, 20, Text.literal("Macro"));
        field.setMaxLength(256);
        field.setText(value == null ? "" : value);
        field.setChangedListener(text -> setter.set(text));
        addDrawableChild(field);
        return field;
    }

    private void initHotkeys(int centerX, int row) {
        addDrawableChild(hotkeyButton(centerX, row, "Open GUI", 1, ClientUtilsMod.CONFIG.openGuiKey));
        row += 22;
        addDrawableChild(hotkeyButton(centerX, row, "Toggle Triggerbot", 2, ClientUtilsMod.CONFIG.toggleTriggerbotKey));
        row += 22;
        addDrawableChild(hotkeyButton(centerX, row, "Macro 1", 3, ClientUtilsMod.CONFIG.macro1Key));
        row += 22;
        addDrawableChild(hotkeyButton(centerX, row, "Macro 2", 4, ClientUtilsMod.CONFIG.macro2Key));
        row += 22;
        addDrawableChild(hotkeyButton(centerX, row, "Macro 3", 5, ClientUtilsMod.CONFIG.macro3Key));
        row += 22;
        addDrawableChild(hotkeyButton(centerX, row, "Select Target", 6, ClientUtilsMod.CONFIG.selectTargetKey));
        row += 22;
        addDrawableChild(hotkeyButton(centerX, row, "Toggle Freecam", 7, ClientUtilsMod.CONFIG.freecamKey));
        row += 22;
        addDrawableChild(hotkeyButton(centerX, row, "Toggle Xray", 8, ClientUtilsMod.CONFIG.xrayToggleKey));
        row += 22;
        addDrawableChild(hotkeyButton(centerX, row, "Auto Pot", 9, ClientUtilsMod.CONFIG.autoPotKey));
        row += 22;
        addDrawableChild(hotkeyButton(centerX, row, "Creative EChest Dump", 10, ClientUtilsMod.CONFIG.creativeEchestDumpKey));
        row += 22;
        addDrawableChild(hotkeyButton(centerX, row, "Open EChest Command", 11, ClientUtilsMod.CONFIG.openEnderChestCommandKey));
    }

    private ButtonWidget hotkeyButton(int centerX, int y, String label, int slot, int key) {
        String keyName = GLFW.glfwGetKeyName(key, 0);
        if (keyName == null) {
            keyName = Integer.toString(key);
        }
        String text = waitingForHotkey == slot ? label + ": press key..." : label + ": " + keyName.toUpperCase();
        return ButtonWidget.builder(Text.literal(text), b -> {
            waitingForHotkey = slot;
            init();
        }).dimensions(centerX - 100, y, 200, 20).build();
    }

    private ButtonWidget toggleButton(int centerX, int y, String name, ToggleGetter getter, ToggleSetter setter) {
        return ButtonWidget.builder(
                label(name, getter.get()),
                button -> {
                    boolean next = !getter.get();
                    setter.set(next);
                    ClientUtilsMod.CONFIG.save();
                    button.setMessage(label(name, next));
                }
        ).dimensions(centerX - 100, y, 200, 20).build();
    }

    private Text label(String feature, boolean enabled) {
        return Text.literal(feature + ": " + (enabled ? "ON" : "OFF"));
    }

    private void switchPage(Page next) {
        this.page = next;
        this.waitingForHotkey = 0;
        init();
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        int keyCode = keyInput.key();
        if (waitingForHotkey > 0) {
            if (waitingForHotkey == 1) {
                ClientUtilsMod.CONFIG.openGuiKey = keyCode;
            } else if (waitingForHotkey == 2) {
                ClientUtilsMod.CONFIG.toggleTriggerbotKey = keyCode;
            } else if (waitingForHotkey == 3) {
                ClientUtilsMod.CONFIG.macro1Key = keyCode;
            } else if (waitingForHotkey == 4) {
                ClientUtilsMod.CONFIG.macro2Key = keyCode;
            } else if (waitingForHotkey == 5) {
                ClientUtilsMod.CONFIG.macro3Key = keyCode;
            } else if (waitingForHotkey == 6) {
                ClientUtilsMod.CONFIG.selectTargetKey = keyCode;
            } else if (waitingForHotkey == 7) {
                ClientUtilsMod.CONFIG.freecamKey = keyCode;
            } else if (waitingForHotkey == 8) {
                ClientUtilsMod.CONFIG.xrayToggleKey = keyCode;
            } else if (waitingForHotkey == 9) {
                ClientUtilsMod.CONFIG.autoPotKey = keyCode;
            } else if (waitingForHotkey == 10) {
                ClientUtilsMod.CONFIG.creativeEchestDumpKey = keyCode;
            } else if (waitingForHotkey == 11) {
                ClientUtilsMod.CONFIG.openEnderChestCommandKey = keyCode;
            }
            waitingForHotkey = 0;
            ClientUtilsMod.CONFIG.save();
            init();
            return true;
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public void close() {
        ClientUtilsMod.CONFIG.save();
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 18, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Page: " + page.title), this.width / 2, 28, 0xAAAAAA);
    }

    private enum Page {
        MODULES("Modules"),
        UTILITY("Utility"),
        COMBAT("Combat"),
        RENDER("Render"),
        FRIENDS("Friends"),
        MACROS("Macros"),
        HOTKEYS("Hotkeys");

        private final String title;

        Page(String title) {
            this.title = title;
        }
    }

    @FunctionalInterface
    private interface ToggleSetter {
        void set(boolean value);
    }

    @FunctionalInterface
    private interface ToggleGetter {
        boolean get();
    }

    @FunctionalInterface
    private interface MacroSetter {
        void set(String text);
    }
}
