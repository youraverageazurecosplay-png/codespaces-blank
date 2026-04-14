package com.example.clientutils;

import com.example.clientutils.config.ClientUtilsConfig;
import com.example.clientutils.feature.FriendManager;
import com.example.clientutils.feature.HotkeyManager;
import com.example.clientutils.feature.MacroRunner;
import com.example.clientutils.feature.PvpTargetManager;
import com.example.clientutils.feature.WaypointManager;
import com.example.clientutils.gui.ClientUtilsScreen;
import com.example.clientutils.integration.JadeBridge;
import com.example.clientutils.integration.LitematicaBridge;
import com.example.clientutils.integration.ReplayModBridge;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.HotbarStorageEntry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.FireworkRocketItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class ClientUtilsMod implements ClientModInitializer {
    public static final String MOD_ID = "clientutils";
    public static ClientUtilsConfig CONFIG;

    private static Double originalGamma = null;
    private static int tickCounter = 0;
    private static boolean wasMiddleClickDown = false;
    private static String lastMainHandItem = "";
    private static final MacroRunner MACRO_RUNNER = new MacroRunner();
    private static final HotkeyManager HOTKEY_MANAGER = new HotkeyManager();
    private static final List<BlockPos> ORE_MARKERS = new ArrayList<>();
    private static final List<BlockPos> STORAGE_MARKERS = new ArrayList<>();
    private static final Set<UUID> GLOWING_PLAYERS = new HashSet<>();

    private static boolean freecamActive;
    private static Vec3d freecamAnchorPos = Vec3d.ZERO;
    private static float freecamAnchorYaw = 0f;
    private static float freecamAnchorPitch = 0f;
    private int lastTrapAlertTick = -1000;
    private int lastVoidPearlWarnTick = -1000;
    private int autoEatReturnSlot = -1;
    private int autoEatSwapBackInvSlot = -1;
    private static final Path PM_LOG = FabricLoader.getInstance().getConfigDir().resolve("clientutils-pm.log");
    private static final DateTimeFormatter PM_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);
    private static boolean freecamPrevAllowFlying;
    private static boolean freecamPrevFlying;
    private String lastServerAddress = "";
    private String lastServerName = "";
    private int disconnectedSinceTick = -1;

    @Override
    public void onInitializeClient() {
        CONFIG = ClientUtilsConfig.load();
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> onIncomingMessage(message.getString()));
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> onIncomingMessage(message.getString()));
    }

    private void onClientTick(MinecraftClient client) {
        tickCounter++;
        trackCurrentServer(client);
        handleReconnectAndRespawn(client);

        if (client.player == null || client.options == null || client.world == null) {
            return;
        }

        HOTKEY_MANAGER.handleHotkeys(client);
        handleSafetyLeave(client);
        applyTweakFeatures(client);
        runCombatFeatures(client);
        runInventoryFeatures(client);
        runUtilitySafety(client);
        runFireworkNoHold(client);
        runFriendTools(client);
        runEspFeatures(client);
        runWaypoints(client);
        MACRO_RUNNER.tick(client);
    }

    private void applyTweakFeatures(MinecraftClient client) {
        if (CONFIG.autoSprintEnabled && client.player.forwardSpeed > 0.8f && !client.player.horizontalCollision) {
            client.player.setSprinting(true);
        }

        if (CONFIG.autoWalkEnabled && client.currentScreen == null) {
            client.options.forwardKey.setPressed(true);
        } else if (!MACRO_RUNNER.isForceWalking()) {
            client.options.forwardKey.setPressed(false);
        }

        if (CONFIG.fullBrightEnabled || CONFIG.xrayEnabled) {
            if (originalGamma == null) {
                originalGamma = client.options.getGamma().getValue();
            }
            client.options.getGamma().setValue(16.0d);
        } else if (originalGamma != null) {
            client.options.getGamma().setValue(originalGamma);
            originalGamma = null;
        }

        if (CONFIG.antiAfkEnabled && tickCounter % 80 == 0) {
            client.player.setYaw(client.player.getYaw() + 1.2f);
        }

        if (CONFIG.freecamEnabled) {
            runFreecam(client);
        } else if (freecamActive) {
            disableFreecam(client);
        }
    }

    private void runCombatFeatures(MinecraftClient client) {
        if (CONFIG.autoAttributeSwapEnabled) {
            autoAttributeSwap(client);
        }
        if (CONFIG.triggerbotEnabled) {
            runTriggerbot(client);
        }

        PlayerEntity target = getSelectedPvpTarget(client);
        if ((CONFIG.autoCrystalEnabled || CONFIG.autoAnchorEnabled || CONFIG.advancedAutoPvpEnabled) && target == null) {
            return;
        }
        if (target != null && (CONFIG.stopWhenTargetDeadEnabled && !target.isAlive())) {
            stopPvpModules(client);
            return;
        }
        if (target != null) {
            if (CONFIG.advancedAutoPvpEnabled) {
                runAdvancedPvp(client, target);
            }
            if (CONFIG.autoCrystalEnabled) {
                runAutoCrystal(client, target);
            }
            if (CONFIG.autoAnchorEnabled) {
                runAutoAnchor(client, target);
            }
        }
    }

    private void runInventoryFeatures(MinecraftClient client) {
        if (CONFIG.autoTotemRefillEnabled && tickCounter % 4 == 0) {
            autoTotemRefill(client);
        }

        if (CONFIG.autoToolSwapEnabled) {
            autoToolSwap(client);
        }

        trackMainHand(client.player);
        if (CONFIG.autoMainHandRefillEnabled && tickCounter % 3 == 0) {
            refillMainHand(client);
        }

        runAutoEat(client);
    }

    private void runUtilitySafety(MinecraftClient client) {
        if (CONFIG.projectileTrajectoryEnabled) {
            renderProjectileTrajectory(client);
        }
        if (CONFIG.voidPearlSafetyEnabled) {
            enforceVoidPearlSafety(client);
        }
        if (CONFIG.trapDetectionEnabled && tickCounter % 40 == 0) {
            runTrapDetection(client);
        }
    }

    private void onIncomingMessage(String rawMessage) {
        if (!CONFIG.privateMessageCaptureEnabled || rawMessage == null || rawMessage.isBlank()) {
            return;
        }
        String lower = rawMessage.toLowerCase(Locale.ROOT).trim();
        boolean looksLikePm = lower.contains(" whispers to you")
                || lower.contains(" tells you")
                || lower.matches("^from\\s+[^:]+:.*")
                || lower.matches("^to\\s+[^:]+:.*")
                || lower.matches("^\\[msg\\].*")
                || lower.matches(".*->\\s*you\\b.*")
                || lower.matches(".*\\byou\\s*<-.*");
        if (!looksLikePm) {
            return;
        }

        String line = "[" + PM_TIME.format(Instant.now()) + " UTC] " + rawMessage + System.lineSeparator();
        try {
            Files.writeString(PM_LOG, line, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception ignored) {
        }
    }

    private void runFireworkNoHold(MinecraftClient client) {
        if (!CONFIG.fireworkWithoutHoldingEnabled || client.interactionManager == null || client.currentScreen != null) {
            return;
        }
        if (!client.player.isGliding()) {
            return;
        }
        if (!client.options.useKey.isPressed()) {
            return;
        }
        if (client.player.getMainHandStack().getItem() instanceof FireworkRocketItem
                || client.player.getOffHandStack().getItem() instanceof FireworkRocketItem) {
            return;
        }
        int rocketSlot = findItemInHotbar(client.player, Items.FIREWORK_ROCKET);
        if (rocketSlot < 0) {
            return;
        }
        int previous = client.player.getInventory().getSelectedSlot();
        client.player.getInventory().setSelectedSlot(rocketSlot);
        client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
        client.player.getInventory().setSelectedSlot(previous);
    }

    private void runFriendTools(MinecraftClient client) {
        if (!CONFIG.middleClickFriendsEnabled || client.crosshairTarget == null) {
            return;
        }
        long handle = client.getWindow().getHandle();
        boolean middleDown = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;
        if (!middleDown || wasMiddleClickDown) {
            wasMiddleClickDown = middleDown;
            return;
        }
        wasMiddleClickDown = true;

        if (!(client.crosshairTarget instanceof EntityHitResult hitResult)) {
            return;
        }
        Entity entity = hitResult.getEntity();
        if (!(entity instanceof PlayerEntity player)) {
            return;
        }

        boolean nowFriend = FriendManager.toggle(player.getName().getString());
        notify(client, player.getName().getString() + (nowFriend ? " added to friends" : " removed from friends"));
    }

    private void runEspFeatures(MinecraftClient client) {
        if (tickCounter % 10 == 0) {
            updateEntityEsp(client);
        }
        if (tickCounter % 40 == 0) {
            updateBlockMarkers(client);
        }
        renderMarkers(client);
    }

    private void updateEntityEsp(MinecraftClient client) {
        if (!(CONFIG.espEnabled || CONFIG.tracersEnabled || CONFIG.advancedAutoPvpEnabled)) {
            clearAllGlowing(client);
            return;
        }
        double rangeSq = CONFIG.espRange * CONFIG.espRange;
        Set<UUID> shouldGlow = new HashSet<>();
        for (var player : client.world.getPlayers()) {
            if (player == client.player) {
                continue;
            }
            double distSq = player.squaredDistanceTo(client.player);
            if (distSq > rangeSq) {
                continue;
            }
            if (FriendManager.isFriend(player.getName().getString())) {
                continue;
            }
            if (CONFIG.espEnabled) {
                player.setGlowing(true);
                shouldGlow.add(player.getUuid());
            }
            if (CONFIG.tracersEnabled && tickCounter % 2 == 0) {
                spawnLineParticles(client, client.player.getEyePos(), new Vec3d(player.getX(), player.getY() + 1.0, player.getZ()), 0.8);
            }
        }

        for (UUID existing : new HashSet<>(GLOWING_PLAYERS)) {
            if (!shouldGlow.contains(existing)) {
                PlayerEntity p = findPlayerByUuid(client, existing);
                if (p != null) {
                    p.setGlowing(false);
                }
                GLOWING_PLAYERS.remove(existing);
            }
        }
        GLOWING_PLAYERS.addAll(shouldGlow);
    }

    private void updateBlockMarkers(MinecraftClient client) {
        ORE_MARKERS.clear();
        STORAGE_MARKERS.clear();
        if (!(CONFIG.blockEspEnabled || CONFIG.storageEspEnabled || CONFIG.xrayEnabled)) {
            return;
        }
        BlockPos center = client.player.getBlockPos();
        int radius = Math.min(10, Math.max(4, CONFIG.espRange / 3));
        int step = 2;

        for (int x = -radius; x <= radius; x += step) {
            for (int y = -radius; y <= radius; y += step) {
                for (int z = -radius; z <= radius; z += step) {
                    BlockPos pos = center.add(x, y, z);
                    BlockState state = client.world.getBlockState(pos);
                    Block block = state.getBlock();

                    if ((CONFIG.blockEspEnabled || CONFIG.xrayEnabled) && isOreOrPvpBlock(block)) {
                        if (ORE_MARKERS.size() < 256) {
                            ORE_MARKERS.add(pos);
                        }
                    }
                    if (CONFIG.storageEspEnabled && isStorageBlock(block)) {
                        if (STORAGE_MARKERS.size() < 256) {
                            STORAGE_MARKERS.add(pos);
                        }
                    }
                }
            }
        }
    }

    private void renderMarkers(MinecraftClient client) {
        if (CONFIG.blockEspEnabled || CONFIG.xrayEnabled) {
            for (int i = 0; i < ORE_MARKERS.size(); i += 8) {
                BlockPos pos = ORE_MARKERS.get(i);
                client.world.addParticleClient(ParticleTypes.END_ROD,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0, 0.01, 0);
            }
        }
        if (CONFIG.storageEspEnabled) {
            for (int i = 0; i < STORAGE_MARKERS.size(); i += 8) {
                BlockPos pos = STORAGE_MARKERS.get(i);
                client.world.addParticleClient(ParticleTypes.HAPPY_VILLAGER,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0, 0.01, 0);
            }
        }
    }

    private void runWaypoints(MinecraftClient client) {
        if (!CONFIG.waypointEnabled) {
            return;
        }
        WaypointManager.Waypoint nearest = WaypointManager.nearest(client.player.getX(), client.player.getY(), client.player.getZ());
        if (nearest == null) {
            return;
        }
        if (tickCounter % 40 == 0) {
            double dx = nearest.x() - client.player.getX();
            double dy = nearest.y() - client.player.getY();
            double dz = nearest.z() - client.player.getZ();
            int dist = (int) Math.sqrt(dx * dx + dy * dy + dz * dz);
            client.player.sendMessage(Text.literal("[Waypoint] " + nearest.name() + " (" + dist + "m)"), true);
        }
        if (tickCounter % 4 == 0) {
            Vec3d start = client.player.getEyePos();
            Vec3d end = new Vec3d(nearest.x() + 0.5, nearest.y() + 1.0, nearest.z() + 0.5);
            spawnLineParticles(client, start, end, 1.2);
        }
    }

    private void renderProjectileTrajectory(MinecraftClient client) {
        if (client.currentScreen != null) {
            return;
        }
        ItemStack stack = client.player.getMainHandStack();
        if (stack.isEmpty() || !isTrajectoryItem(stack)) {
            return;
        }

        Vec3d position = client.player.getEyePos().add(client.player.getRotationVec(1.0f).multiply(0.2));
        Vec3d velocity = client.player.getRotationVec(1.0f).multiply(getProjectileSpeed(client, stack));
        double gravity = getProjectileGravity(stack);

        for (int i = 0; i < 45; i++) {
            Vec3d next = position.add(velocity);
            BlockHitResult hit = client.world.raycast(new RaycastContext(
                    position, next, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, client.player
            ));

            if (hit.getType() == HitResult.Type.BLOCK) {
                Vec3d impact = hit.getPos();
                client.world.addParticleClient(ParticleTypes.CRIT, impact.x, impact.y, impact.z, 0, 0, 0);
                break;
            }

            client.world.addParticleClient(ParticleTypes.SMOKE, next.x, next.y, next.z, 0, 0, 0);
            position = next;
            velocity = velocity.multiply(0.99);
            velocity = new Vec3d(velocity.x, velocity.y - gravity, velocity.z);

            if (position.y < client.world.getBottomY() - 5) {
                break;
            }
        }
    }

    private void enforceVoidPearlSafety(MinecraftClient client) {
        if (!client.options.useKey.isPressed()) {
            return;
        }
        if (isKeyDown(client, GLFW.GLFW_KEY_0)) {
            return;
        }
        if (!isHoldingPearl(client.player)) {
            return;
        }

        double landingY = predictPearlLandingY(client);
        if (landingY <= client.world.getBottomY() + 1) {
            client.options.useKey.setPressed(false);
            if (tickCounter - lastVoidPearlWarnTick > 30) {
                notify(client, "Void pearl prevented. Hold 0 while throwing to override.");
                lastVoidPearlWarnTick = tickCounter;
            }
        }
    }

    private double predictPearlLandingY(MinecraftClient client) {
        Vec3d position = client.player.getEyePos().add(client.player.getRotationVec(1.0f).multiply(0.2));
        Vec3d velocity = client.player.getRotationVec(1.0f).multiply(1.5);
        for (int i = 0; i < 80; i++) {
            Vec3d next = position.add(velocity);
            BlockHitResult hit = client.world.raycast(new RaycastContext(
                    position, next, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, client.player
            ));
            if (hit.getType() == HitResult.Type.BLOCK) {
                return hit.getPos().y;
            }
            position = next;
            velocity = velocity.multiply(0.99);
            velocity = new Vec3d(velocity.x, velocity.y - 0.03, velocity.z);
            if (position.y < client.world.getBottomY() - 5) {
                return position.y;
            }
        }
        return position.y;
    }

    private void runTrapDetection(MinecraftClient client) {
        int tntMinecarts = 0;
        int arrows = 0;
        int craftingTables = 0;
        int buttons = 0;
        int dispensers = 0;

        Box entityBox = client.player.getBoundingBox().expand(18);
        for (Entity entity : client.world.getOtherEntities(client.player, entityBox, e -> true)) {
            String type = entity.getType().toString().toLowerCase(Locale.ROOT);
            if (type.contains("tnt_minecart")) {
                tntMinecarts++;
            }
            if (type.contains("arrow")) {
                arrows++;
            }
        }

        BlockPos center = client.player.getBlockPos();
        int radius = 10;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -4; y <= 6; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = client.world.getBlockState(center.add(x, y, z)).getBlock();
                    String name = block.asItem().toString().toLowerCase(Locale.ROOT);
                    if (name.contains("crafting_table")) {
                        craftingTables++;
                    }
                    if (name.contains("button")) {
                        buttons++;
                    }
                    if (name.contains("dispenser")) {
                        dispensers++;
                    }
                }
            }
        }

        boolean suspicious = tntMinecarts >= 8 || arrows >= 20 || craftingTables >= 10 || buttons >= 30 || dispensers >= 8;
        if (suspicious && tickCounter - lastTrapAlertTick > 80) {
            notify(client, "Trap risk: TNTc=" + tntMinecarts + " arrows=" + arrows
                    + " craft=" + craftingTables + " buttons=" + buttons + " disp=" + dispensers);
            lastTrapAlertTick = tickCounter;
        }
    }

    public static void triggerAutoPot(MinecraftClient client) {
        if (client.interactionManager == null || client.currentScreen != null) {
            return;
        }
        int selected = client.player.getInventory().getSelectedSlot();
        int hotbarSlot = findPotionInHotbar(client.player);
        int inventorySlot = -1;

        if (hotbarSlot < 0) {
            inventorySlot = findPotionInInventory(client.player);
            if (inventorySlot < 0) {
                notify(client, "AutoPot: no matching splash potion found.");
                return;
            }
            int syncId = client.player.currentScreenHandler.syncId;
            client.interactionManager.clickSlot(syncId, inventorySlotToContainerSlot(inventorySlot), selected,
                    SlotActionType.SWAP, client.player);
            hotbarSlot = selected;
        }

        int previous = client.player.getInventory().getSelectedSlot();
        float previousPitch = client.player.getPitch();
        client.player.getInventory().setSelectedSlot(hotbarSlot);
        client.player.setPitch(89f);
        client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
        client.player.swingHand(Hand.MAIN_HAND);
        client.player.setPitch(previousPitch);
        client.player.getInventory().setSelectedSlot(previous);

        if (inventorySlot >= 0) {
            int syncId = client.player.currentScreenHandler.syncId;
            client.interactionManager.clickSlot(syncId, inventorySlotToContainerSlot(inventorySlot), selected,
                    SlotActionType.SWAP, client.player);
        }
    }

    private void runAutoEat(MinecraftClient client) {
        if (!CONFIG.autoEatEnabled || client.interactionManager == null || client.currentScreen != null) {
            return;
        }
        if (autoEatReturnSlot >= 0 && !client.player.isUsingItem()) {
            finishAutoEatRestore(client);
        }
        if (client.player.isUsingItem()) {
            return;
        }
        if (client.player.getHungerManager().getFoodLevel() > CONFIG.autoEatHungerThreshold) {
            return;
        }
        if (tickCounter % 6 != 0) {
            return;
        }

        int selected = client.player.getInventory().getSelectedSlot();
        int foodHotbar = findFoodInHotbar(client.player);
        if (foodHotbar < 0) {
            int invFood = findFoodInInventory(client.player);
            if (invFood < 0) {
                return;
            }
            int syncId = client.player.currentScreenHandler.syncId;
            client.interactionManager.clickSlot(syncId, inventorySlotToContainerSlot(invFood), selected,
                    SlotActionType.SWAP, client.player);
            foodHotbar = selected;
            autoEatSwapBackInvSlot = invFood;
            autoEatReturnSlot = selected;
        } else if (foodHotbar != selected) {
            autoEatReturnSlot = selected;
            autoEatSwapBackInvSlot = -1;
        }

        client.player.getInventory().setSelectedSlot(foodHotbar);
        client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
    }

    private void finishAutoEatRestore(MinecraftClient client) {
        if (autoEatSwapBackInvSlot >= 0) {
            int syncId = client.player.currentScreenHandler.syncId;
            int selected = client.player.getInventory().getSelectedSlot();
            client.interactionManager.clickSlot(syncId, inventorySlotToContainerSlot(autoEatSwapBackInvSlot), selected,
                    SlotActionType.SWAP, client.player);
        }
        if (autoEatReturnSlot >= 0) {
            client.player.getInventory().setSelectedSlot(autoEatReturnSlot);
        }
        autoEatReturnSlot = -1;
        autoEatSwapBackInvSlot = -1;
    }

    public static void runCreativeSavedHotbarDump(MinecraftClient client) {
        if (client.interactionManager == null || client.player == null || !client.player.isCreative()) {
            return;
        }
        if (!(client.currentScreen instanceof GenericContainerScreen)
                || !(client.player.currentScreenHandler instanceof net.minecraft.screen.GenericContainerScreenHandler handler)) {
            notify(client, "Open your ender chest first.");
            return;
        }
        if (!isLikelyEnderChest(handler, client)) {
            notify(client, "This quick dump is only for ender chest.");
            return;
        }

        int rowIndex = MathHelper.clamp(CONFIG.creativeSavedHotbarIndex, 0, 8);
        int slotIndex = MathHelper.clamp(CONFIG.creativeSavedHotbarSlot, 0, 8);
        if (client.getNetworkHandler() == null) {
            return;
        }

        HotbarStorageEntry saved = client.getCreativeHotbarStorage().getSavedHotbar(rowIndex);
        List<ItemStack> stacks = saved.deserialize(client.getNetworkHandler().getRegistryManager());
        if (slotIndex >= stacks.size()) {
            notify(client, "Saved hotbar slot is empty.");
            return;
        }

        ItemStack template = stacks.get(slotIndex);
        if (template == null || template.isEmpty()) {
            notify(client, "Saved hotbar slot is empty.");
            return;
        }

        int selected = client.player.getInventory().getSelectedSlot();
        client.interactionManager.clickCreativeStack(template.copy(), 36 + selected);

        int chestRows = handler.getRows();
        int playerInvContainerSlot = playerInvSlotToContainerSlotWithRows(selected, chestRows);
        client.interactionManager.clickSlot(handler.syncId, playerInvContainerSlot, 0, SlotActionType.QUICK_MOVE, client.player);
    }

    public static int playerInvSlotToContainerSlotWithRows(int invSlot, int rows) {
        int chestSlots = rows * 9;
        if (invSlot < 9) {
            return chestSlots + 27 + invSlot;
        }
        return chestSlots + (invSlot - 9);
    }

    public static boolean isLikelyEnderChest(net.minecraft.screen.GenericContainerScreenHandler handler, MinecraftClient client) {
        if (client.player != null && handler.getInventory() == client.player.getEnderChestInventory()) {
            return true;
        }
        if (client.currentScreen != null) {
            String title = client.currentScreen.getTitle().getString().toLowerCase(Locale.ROOT);
            return title.contains("ender");
        }
        return false;
    }

    private void runTriggerbot(MinecraftClient client) {
        if (client.interactionManager == null || client.currentScreen != null) {
            return;
        }
        if (!(client.crosshairTarget instanceof EntityHitResult entityHit)) {
            return;
        }

        Entity target = entityHit.getEntity();
        if (!(target instanceof LivingEntity) || target == client.player || !target.isAlive()) {
            return;
        }

        if (target instanceof PlayerEntity p && FriendManager.isFriend(p.getName().getString())) {
            return;
        }

        if (!isWeaponInMainHand(client.player)) {
            return;
        }
        if (client.player.getAttackCooldownProgress(0.5f) < 1.0f) {
            return;
        }

        client.interactionManager.attackEntity(client.player, target);
        client.player.swingHand(Hand.MAIN_HAND);
    }

    private void runAdvancedPvp(MinecraftClient client, PlayerEntity target) {
        smoothAimTowards(client, target);
        if (client.player.squaredDistanceTo(target) <= CONFIG.combatRange * CONFIG.combatRange
                && client.player.getAttackCooldownProgress(0.5f) >= 1.0f
                && isWeaponInMainHand(client.player)) {
            client.interactionManager.attackEntity(client.player, target);
            client.player.swingHand(Hand.MAIN_HAND);
        }
    }

    private void runAutoCrystal(MinecraftClient client, PlayerEntity target) {
        smoothAimTowards(client, target);
        maybePearlTowardsTarget(client, target);

        int crystalSlot = ensureInHotbar(client, Items.END_CRYSTAL);
        int obsidianSlot = ensureInHotbar(client, Items.OBSIDIAN);
        if (crystalSlot < 0) {
            return;
        }

        BlockPos base = findCrystalBaseNearTarget(client, target);
        if (base == null && obsidianSlot >= 0) {
            base = target.getBlockPos().down();
            if (canPlaceOn(client, base) && inRange(client, base)) {
                placeBlockAt(client, obsidianSlot, base);
            }
        }
        if (base != null && inRange(client, base)) {
            placeOnBlock(client, crystalSlot, base);
        }

        EndCrystalEntity crystal = nearestCrystal(client, target);
        if (crystal != null && crystal.isAlive() && client.player.getAttackCooldownProgress(0.5f) >= 0.85f) {
            client.interactionManager.attackEntity(client.player, crystal);
            client.player.swingHand(Hand.MAIN_HAND);
        }
    }

    private void runAutoAnchor(MinecraftClient client, PlayerEntity target) {
        smoothAimTowards(client, target);
        maybePearlTowardsTarget(client, target);

        int anchorSlot = ensureInHotbar(client, Items.RESPAWN_ANCHOR);
        int glowstoneSlot = ensureInHotbar(client, Items.GLOWSTONE);
        if (anchorSlot < 0 || glowstoneSlot < 0) {
            return;
        }

        BlockPos targetPos = target.getBlockPos().down();
        if (!inRange(client, targetPos)) {
            return;
        }
        if (client.world.getBlockState(targetPos).isAir()) {
            placeBlockAt(client, anchorSlot, targetPos);
        }
        if (client.world.getBlockState(targetPos).isOf(Blocks.RESPAWN_ANCHOR)) {
            placeOnBlock(client, glowstoneSlot, targetPos);
            placeOnBlock(client, anchorSlot, targetPos);
        }
    }

    private void maybePearlTowardsTarget(MinecraftClient client, PlayerEntity target) {
        if (client.player.squaredDistanceTo(target) <= CONFIG.combatRange * CONFIG.combatRange) {
            return;
        }
        if (tickCounter % 20 != 0) {
            return;
        }
        int pearlSlot = findItemInHotbar(client.player, Items.ENDER_PEARL);
        if (pearlSlot < 0) {
            return;
        }
        smoothAimTowards(client, target);
        int previous = client.player.getInventory().getSelectedSlot();
        client.player.getInventory().setSelectedSlot(pearlSlot);
        client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
        client.player.getInventory().setSelectedSlot(previous);
    }

    private void stopPvpModules(MinecraftClient client) {
        if (!CONFIG.stopWhenTargetDeadEnabled) {
            return;
        }
        CONFIG.autoCrystalEnabled = false;
        CONFIG.autoAnchorEnabled = false;
        CONFIG.advancedAutoPvpEnabled = false;
        PvpTargetManager.clear();
        CONFIG.save();
        notify(client, "Target down. Auto PvP modules stopped.");
    }

    private void smoothAimTowards(MinecraftClient client, Entity target) {
        if (!CONFIG.smoothAimEnabled || target == null) {
            return;
        }
        Vec3d eye = client.player.getEyePos();
        Vec3d targetPos = new Vec3d(target.getX(), target.getY() + target.getHeight() * 0.6, target.getZ());
        Vec3d delta = targetPos.subtract(eye);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);

        float targetYaw = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0);
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(delta.y, horizontal)));

        float yaw = client.player.getYaw();
        float pitch = client.player.getPitch();
        float smooth = 0.22f;

        client.player.setYaw(lerpAngle(yaw, targetYaw, smooth));
        client.player.setPitch(MathHelper.lerp(smooth, pitch, targetPitch));
    }

    private float lerpAngle(float start, float end, float t) {
        float delta = MathHelper.wrapDegrees(end - start);
        return start + delta * t;
    }

    private PlayerEntity getSelectedPvpTarget(MinecraftClient client) {
        UUID uuid = PvpTargetManager.getTargetUuid();
        if (uuid == null) {
            return null;
        }
        for (var player : client.world.getPlayers()) {
            if (player.getUuid().equals(uuid)) {
                return player;
            }
        }
        return null;
    }

    public static void selectPvpTargetUnderCrosshair(MinecraftClient client) {
        if (!(client.crosshairTarget instanceof EntityHitResult hitResult)) {
            return;
        }
        if (!(hitResult.getEntity() instanceof PlayerEntity player) || player == client.player) {
            return;
        }
        PvpTargetManager.select(player);
        notify(client, "Selected target: " + player.getName().getString());
    }

    private void runFreecam(MinecraftClient client) {
        if (!freecamActive) {
            enableFreecam(client);
        }
        client.player.noClip = true;
        client.player.getAbilities().allowFlying = true;
        client.player.getAbilities().flying = true;
    }

    public static void enableFreecam(MinecraftClient client) {
        freecamAnchorPos = new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ());
        freecamAnchorYaw = client.player.getYaw();
        freecamAnchorPitch = client.player.getPitch();
        freecamPrevAllowFlying = client.player.getAbilities().allowFlying;
        freecamPrevFlying = client.player.getAbilities().flying;
        freecamActive = true;
    }

    public static void disableFreecam(MinecraftClient client) {
        client.player.noClip = false;
        client.player.getAbilities().allowFlying = freecamPrevAllowFlying;
        client.player.getAbilities().flying = freecamPrevFlying;
        freecamActive = false;
        freecamAnchorPos = Vec3d.ZERO;
    }

    private BlockPos findCrystalBaseNearTarget(MinecraftClient client, PlayerEntity target) {
        BlockPos center = target.getBlockPos().down();
        List<BlockPos> candidates = List.of(
                center,
                center.north(),
                center.south(),
                center.east(),
                center.west(),
                center.north().east(),
                center.north().west(),
                center.south().east(),
                center.south().west()
        );
        for (BlockPos pos : candidates) {
            if (!inRange(client, pos)) {
                continue;
            }
            BlockState base = client.world.getBlockState(pos);
            if ((base.isOf(Blocks.OBSIDIAN) || base.isOf(Blocks.BEDROCK))
                    && client.world.getBlockState(pos.up()).isAir()
                    && client.world.getBlockState(pos.up(2)).isAir()) {
                return pos;
            }
        }
        return null;
    }

    private EndCrystalEntity nearestCrystal(MinecraftClient client, PlayerEntity target) {
        Box box = target.getBoundingBox().expand(5.0);
        EndCrystalEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity entity : client.world.getOtherEntities(client.player, box, e -> e instanceof EndCrystalEntity)) {
            double dist = entity.squaredDistanceTo(client.player);
            if (dist < bestDist) {
                bestDist = dist;
                best = (EndCrystalEntity) entity;
            }
        }
        return best;
    }

    private boolean canPlaceOn(MinecraftClient client, BlockPos pos) {
        if (!client.world.getBlockState(pos).isAir()) {
            return false;
        }
        return client.world.getBlockState(pos.down()).isOpaque();
    }

    private boolean inRange(MinecraftClient client, BlockPos pos) {
        return client.player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                <= CONFIG.combatRange * CONFIG.combatRange;
    }

    private void placeBlockAt(MinecraftClient client, int hotbarSlot, BlockPos pos) {
        BlockPos support = pos.down();
        if (client.world.getBlockState(support).isAir()) {
            return;
        }
        placeOnBlock(client, hotbarSlot, support);
    }

    private void placeOnBlock(MinecraftClient client, int hotbarSlot, BlockPos basePos) {
        if (hotbarSlot < 0 || client.interactionManager == null) {
            return;
        }
        int previous = client.player.getInventory().getSelectedSlot();
        client.player.getInventory().setSelectedSlot(hotbarSlot);

        Vec3d hit = Vec3d.ofCenter(basePos).add(0, 0.5, 0);
        BlockHitResult hitResult = new BlockHitResult(hit, Direction.UP, basePos, false);
        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitResult);
        client.player.swingHand(Hand.MAIN_HAND);
        client.player.getInventory().setSelectedSlot(previous);
    }

    private int ensureInHotbar(MinecraftClient client, Item item) {
        int inHotbar = findItemInHotbar(client.player, item);
        if (inHotbar >= 0) {
            return inHotbar;
        }
        int inInventory = findItemInInventory(client.player, item);
        if (inInventory < 0) {
            return -1;
        }
        int selected = client.player.getInventory().getSelectedSlot();
        int syncId = client.player.currentScreenHandler.syncId;
        client.interactionManager.clickSlot(syncId, inventorySlotToContainerSlot(inInventory), selected, SlotActionType.SWAP, client.player);
        return selected;
    }

    private void autoAttributeSwap(MinecraftClient client) {
        if (!(client.crosshairTarget instanceof EntityHitResult)) {
            return;
        }
        int bestSlot = client.player.getInventory().getSelectedSlot();
        double bestScore = getMainHandAttackScore(client.player.getInventory().getStack(bestSlot));

        for (int slot : parseSlots(CONFIG.attributeSwapSlots)) {
            if (slot < 0 || slot > 8) {
                continue;
            }
            double score = getMainHandAttackScore(client.player.getInventory().getStack(slot));
            if (score > bestScore) {
                bestScore = score;
                bestSlot = slot;
            }
        }
        client.player.getInventory().setSelectedSlot(bestSlot);
    }

    private List<Integer> parseSlots(String csv) {
        List<Integer> out = new ArrayList<>();
        if (csv == null || csv.isBlank()) {
            return out;
        }
        for (String token : csv.split(",")) {
            try {
                int parsed = Integer.parseInt(token.trim()) - 1;
                out.add(parsed);
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }

    private double getMainHandAttackScore(ItemStack stack) {
        if (stack.isEmpty()) {
            return -1000.0;
        }
        final double[] score = {0.0};
        stack.applyAttributeModifiers(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.equals(EntityAttributes.ATTACK_DAMAGE)) {
                score[0] += modifier.value();
            }
            if (attribute.equals(EntityAttributes.ATTACK_SPEED)) {
                score[0] += modifier.value() * 0.25d;
            }
        });
        return score[0];
    }

    private void autoTotemRefill(MinecraftClient client) {
        if (!client.player.getOffHandStack().isEmpty() || client.interactionManager == null) {
            return;
        }
        int invSlot = findFirstInInventoryByName(client.player, "totem_of_undying");
        if (invSlot < 0) {
            return;
        }
        int slotId = inventorySlotToContainerSlot(invSlot);
        int syncId = client.player.currentScreenHandler.syncId;
        client.interactionManager.clickSlot(syncId, slotId, 0, SlotActionType.PICKUP, client.player);
        client.interactionManager.clickSlot(syncId, 45, 0, SlotActionType.PICKUP, client.player);
        client.interactionManager.clickSlot(syncId, slotId, 0, SlotActionType.PICKUP, client.player);
    }

    private void autoToolSwap(MinecraftClient client) {
        if (!(client.crosshairTarget instanceof BlockHitResult blockHitResult)) {
            return;
        }
        if (!client.options.attackKey.isPressed()) {
            return;
        }
        BlockState state = client.world.getBlockState(blockHitResult.getBlockPos());
        int bestSlot = client.player.getInventory().getSelectedSlot();
        float bestSpeed = client.player.getInventory().getStack(bestSlot).getMiningSpeedMultiplier(state);
        for (int slot = 0; slot < 9; slot++) {
            float speed = client.player.getInventory().getStack(slot).getMiningSpeedMultiplier(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = slot;
            }
        }
        client.player.getInventory().setSelectedSlot(bestSlot);
    }

    private void trackMainHand(PlayerEntity player) {
        ItemStack main = player.getMainHandStack();
        if (!main.isEmpty()) {
            lastMainHandItem = main.getItem().toString();
        }
    }

    private void refillMainHand(MinecraftClient client) {
        int selected = client.player.getInventory().getSelectedSlot();
        ItemStack current = client.player.getInventory().getStack(selected);
        if (!current.isEmpty()) {
            return;
        }
        int refillFrom = -1;
        int bestCount = 0;
        for (int i = 9; i < client.player.getInventory().size(); i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (!stack.getItem().toString().equals(lastMainHandItem)) {
                continue;
            }
            if (stack.getCount() > bestCount) {
                bestCount = stack.getCount();
                refillFrom = i;
            }
        }
        if (refillFrom < 0) {
            return;
        }
        int syncId = client.player.currentScreenHandler.syncId;
        int fromSlotId = inventorySlotToContainerSlot(refillFrom);
        client.interactionManager.clickSlot(syncId, fromSlotId, selected, SlotActionType.SWAP, client.player);
    }

    private int findItemInHotbar(PlayerEntity player, Item item) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.isOf(item)) {
                return i;
            }
        }
        return -1;
    }

    private int findItemInInventory(PlayerEntity player, Item item) {
        for (int i = 9; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.isOf(item)) {
                return i;
            }
        }
        return -1;
    }

    public static int findPotionInHotbar(PlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (isPreferredPotion(stack)) {
                return i;
            }
        }
        return -1;
    }

    public static int findPotionInInventory(PlayerEntity player) {
        for (int i = 9; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (isPreferredPotion(stack)) {
                return i;
            }
        }
        return -1;
    }

    public static boolean isPreferredPotion(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        String itemId = stack.getItem().toString().toLowerCase(Locale.ROOT);
        if (!itemId.contains("splash_potion") && !itemId.contains("lingering_potion")) {
            return false;
        }
        String label = stack.getName().getString().toLowerCase(Locale.ROOT);
        for (String token : CONFIG.autoPotPriority.split(",")) {
            String normalized = token.trim().toLowerCase(Locale.ROOT);
            if (!normalized.isBlank() && label.contains(normalized)) {
                return true;
            }
        }
        return true;
    }

    private int findFoodInHotbar(PlayerEntity player) {
        for (String token : List.of("golden_carrot", "cooked_beef", "steak", "cooked_porkchop", "bread", "cooked_chicken")) {
            int slot = findByNameInHotbar(player, token);
            if (slot >= 0) {
                return slot;
            }
        }
        return -1;
    }

    private int findFoodInInventory(PlayerEntity player) {
        for (String token : List.of("golden_carrot", "cooked_beef", "steak", "cooked_porkchop", "bread", "cooked_chicken")) {
            int slot = findByNameInInventory(player, token);
            if (slot >= 0) {
                return slot;
            }
        }
        return -1;
    }

    private int findByNameInHotbar(PlayerEntity player, String token) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem().toString().toLowerCase(Locale.ROOT).contains(token)) {
                return i;
            }
        }
        return -1;
    }

    private int findByNameInInventory(PlayerEntity player, String token) {
        for (int i = 9; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem().toString().toLowerCase(Locale.ROOT).contains(token)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isTrajectoryItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        String id = stack.getItem().toString().toLowerCase(Locale.ROOT);
        return id.contains("ender_pearl")
                || id.contains("snowball")
                || id.contains("egg")
                || id.contains("experience_bottle")
                || id.contains("splash_potion")
                || id.contains("lingering_potion")
                || id.contains("trident")
                || id.contains("bow");
    }

    private double getProjectileSpeed(MinecraftClient client, ItemStack stack) {
        String id = stack.getItem().toString().toLowerCase(Locale.ROOT);
        if (id.contains("ender_pearl")) {
            return 1.5;
        }
        if (id.contains("splash_potion") || id.contains("lingering_potion")) {
            return 0.5;
        }
        if (id.contains("experience_bottle")) {
            return 0.7;
        }
        if (id.contains("trident")) {
            int maxUse = stack.getMaxUseTime(client.player);
            int use = Math.max(0, maxUse - client.player.getItemUseTimeLeft());
            float pull = Math.min(1.0f, use / 10.0f);
            return 1.8 + (pull * 0.9);
        }
        if (id.contains("bow")) {
            int maxUse = stack.getMaxUseTime(client.player);
            int use = Math.max(0, maxUse - client.player.getItemUseTimeLeft());
            float pull = use / 20.0f;
            pull = (pull * pull + pull * 2.0f) / 3.0f;
            pull = Math.min(1.0f, pull);
            return 0.4 + (pull * 2.6);
        }
        return 1.5;
    }

    private double getProjectileGravity(ItemStack stack) {
        String id = stack.getItem().toString().toLowerCase(Locale.ROOT);
        if (id.contains("trident")) {
            return 0.05;
        }
        if (id.contains("bow")) {
            return 0.05;
        }
        return 0.03;
    }

    private boolean isHoldingPearl(PlayerEntity player) {
        return player.getMainHandStack().isOf(Items.ENDER_PEARL) || player.getOffHandStack().isOf(Items.ENDER_PEARL);
    }

    private int findFirstInInventoryByName(PlayerEntity player, String token) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) {
                continue;
            }
            String name = stack.getItem().toString().toLowerCase(Locale.ROOT);
            if (name.contains(token)) {
                return i;
            }
        }
        return -1;
    }

    public static int inventorySlotToContainerSlot(int invSlot) {
        if (invSlot >= 0 && invSlot <= 8) {
            return 36 + invSlot;
        }
        return invSlot;
    }

    private boolean isWeaponInMainHand(PlayerEntity player) {
        return getMainHandAttackScore(player.getMainHandStack()) > 0.0d
                || player.getMainHandStack().getItem() instanceof AxeItem;
    }

    private boolean isKeyDown(MinecraftClient client, int keyCode) {
        if (keyCode <= 0) {
            return false;
        }
        return GLFW.glfwGetKey(client.getWindow().getHandle(), keyCode) == GLFW.GLFW_PRESS;
    }

    private boolean isStorageBlock(Block block) {
        return block instanceof ChestBlock
                || block instanceof ShulkerBoxBlock
                || block == Blocks.BARREL
                || block == Blocks.ENDER_CHEST;
    }

    private boolean isOreOrPvpBlock(Block block) {
        String id = block.asItem().toString().toLowerCase(Locale.ROOT);
        if (id.contains("_ore") || id.contains("ancient_debris")) {
            return true;
        }
        return CONFIG.clearGlassEnabled && id.contains("glass");
    }

    private void spawnLineParticles(MinecraftClient client, Vec3d start, Vec3d end, double stepDivisor) {
        Vec3d delta = end.subtract(start);
        double length = delta.length();
        if (length < 0.1) {
            return;
        }
        Vec3d dir = delta.normalize();
        for (double d = 0; d < length; d += stepDivisor) {
            Vec3d p = start.add(dir.multiply(d));
            client.world.addParticleClient(ParticleTypes.CRIT, p.x, p.y, p.z, 0, 0, 0);
        }
    }

    private void trackCurrentServer(MinecraftClient client) {
        ServerInfo info = client.getCurrentServerEntry();
        if (info != null && info.address != null && !info.address.isBlank()) {
            lastServerAddress = info.address;
            lastServerName = (info.name == null || info.name.isBlank()) ? info.address : info.name;
        }
    }

    private void handleReconnectAndRespawn(MinecraftClient client) {
        if (client.player != null && CONFIG.autoRespawnEnabled && client.player.isDead()) {
            client.player.requestRespawn();
        }

        if (!(client.currentScreen instanceof DisconnectedScreen)) {
            disconnectedSinceTick = -1;
            return;
        }
        if (!CONFIG.autoRejoinEnabled || lastServerAddress.isBlank()) {
            return;
        }
        if (disconnectedSinceTick < 0) {
            disconnectedSinceTick = tickCounter;
            return;
        }
        int waitTicks = Math.max(1, CONFIG.autoRejoinDelaySeconds) * 20;
        if (tickCounter - disconnectedSinceTick < waitTicks) {
            return;
        }
        disconnectedSinceTick = tickCounter + 20;
        ServerAddress address = ServerAddress.parse(lastServerAddress);
        ServerInfo info = new ServerInfo(lastServerName, lastServerAddress, ServerInfo.ServerType.OTHER);
        ConnectScreen.connect(client.currentScreen, client, address, info, false, null);
    }

    private void handleSafetyLeave(MinecraftClient client) {
        if (!CONFIG.autoLeaveLowHealthEnabled || client.player == null || client.getNetworkHandler() == null) {
            return;
        }
        float hp = client.player.getHealth() + client.player.getAbsorptionAmount();
        if (hp > CONFIG.lowHealthLeaveThreshold) {
            return;
        }
        client.disconnect(Text.literal("Auto leave: low health threshold reached."));
    }

    public static void runEnderChestCommand(MinecraftClient client) {
        if (client.getNetworkHandler() == null || CONFIG.enderChestCommand == null || CONFIG.enderChestCommand.isBlank()) {
            return;
        }
        String command = CONFIG.enderChestCommand.trim();
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        if (!command.isBlank()) {
            client.getNetworkHandler().sendChatCommand(command);
        }
    }

    private PlayerEntity findPlayerByUuid(MinecraftClient client, UUID uuid) {
        for (PlayerEntity player : client.world.getPlayers()) {
            if (player.getUuid().equals(uuid)) {
                return player;
            }
        }
        return null;
    }

    private void clearAllGlowing(MinecraftClient client) {
        for (UUID uuid : new HashSet<>(GLOWING_PLAYERS)) {
            PlayerEntity p = findPlayerByUuid(client, uuid);
            if (p != null) {
                p.setGlowing(false);
            }
        }
        GLOWING_PLAYERS.clear();
    }

    public static void notify(MinecraftClient client, String message) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal("[ClientUtils] " + message), true);
        }
    }

    public static boolean isLitematicaLoaded() {
        return FabricLoader.getInstance().isModLoaded("litematica") && FabricLoader.getInstance().isModLoaded("malilib");
    }

    public static boolean isJadeLoaded() {
        return JadeBridge.isLoaded();
    }

    public static boolean isReplayModLoaded() {
        return ReplayModBridge.isLoaded();
    }

    public static void openLitematicaGuide(MinecraftClient client) {
        LitematicaBridge.openGuide(client);
    }

    public static void openJadeGuide(MinecraftClient client) {
        JadeBridge.openGuide(client);
    }

    public static void openReplayModGuide(MinecraftClient client) {
        ReplayModBridge.openGuide(client);
    }
}
