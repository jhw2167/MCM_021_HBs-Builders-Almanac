package com.holybuckets.builder.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.holybuckets.builder.Constants;
import com.holybuckets.builder.almanac.AlmanacCatalog;
import com.holybuckets.builder.almanac.AlmanacEntry;
import com.holybuckets.builder.core.BuildLayout;
import com.holybuckets.builder.core.BuildManager;
import com.holybuckets.builder.core.BuildOrientation;
import com.holybuckets.builder.item.BuildersAlmanacItem;
import com.holybuckets.builder.networking.BuildLayoutMessage;
import com.holybuckets.foundation.client.ClientEventRegistrar;
import com.holybuckets.foundation.event.custom.RenderLevelEvent;
import com.holybuckets.foundation.event.custom.SimpleMessageEvent;
import com.holybuckets.foundation.networking.SimpleStringMessage;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.client.GuiDrawEvent;
import net.blay09.mods.balm.api.event.client.KeyInputEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client side of build mode: mirrors the server's BuildManager state and layout, renders the preview,
 * and handles distance (scroll), rotation (Q/E) and placement (left click).
 */
public class ClientBuilderManager {

    private static final int DEFAULT_DISTANCE = 8;
    private static final int DEFAULT_HEIGHT = 0;
    private static final int MIN_DISTANCE = 2;
    private static final int MAX_DISTANCE = 16;
    private static final String[] OVERLAY_LINES = {"exit", "place", "distance", "rotate", "undo"};
    private static final ItemStack[] OVERLAY_ICONS = {
        new ItemStack(Items.BARRIER), new ItemStack(Items.BRICKS), new ItemStack(Items.SPYGLASS),
        new ItemStack(Items.COMPASS), new ItemStack(Items.CLOCK)
    };

    private static int buildMode = 0;
    private static String buildId = "";
    private static final Map<BlockState, List<BlockPos>> cache = new HashMap<>();
    private static BuildOrientation orientation = BuildOrientation.IDENTITY;
    private static int distance = DEFAULT_DISTANCE;
    private static int height = DEFAULT_HEIGHT;

    // Render data derived from the cache, rebuilt when the last batch arrives or the build rotates
    private static Map<BlockPos, BlockState> blocks = Map.of();
    private static List<BlockPos> visibleBlocks = List.of();
    private static int buildHeight = 1;

    public static void init(ClientEventRegistrar reg) {
        reg.registerOnSimpleMessage(BuildManager.MSG_BUILD_MODE, ClientBuilderManager::onBuildModeMessage);
        reg.registerOnRenderLevel(RenderLevelEvent.RenderStage.AFTER_TRANSLUCENT_BLOCKS, ClientBuilderManager::onRenderLevel);
        reg.registerOnDisconnectedFromServer(e -> {
            buildMode = 0;
            flushCache();
        });
        reg.registerOnGuiDrawPost(ClientBuilderManager::onGuiDraw);
        Balm.getEvents().onEvent(KeyInputEvent.class, ClientBuilderManager::onKeyInput);
    }

    public static boolean isBuildMode() {
        return buildMode != 0;
    }

    public static boolean isActive() {
        Minecraft mc = Minecraft.getInstance();
        return buildMode != 0 && !blocks.isEmpty() && mc.player != null && mc.screen == null
            && BuildersAlmanacItem.isHeld(mc.player);
    }

    //** Server sync

    private static void onBuildModeMessage(SimpleMessageEvent event) {
        JsonObject json = JsonParser.parseString(event.getContent()).getAsJsonObject();
        boolean active = json.get("buildMode").getAsBoolean();

        if(buildMode==0) {
            if(active) {
                distance = DEFAULT_DISTANCE;
                buildMode=1;
            }
        } else {
            if(!active) {
                buildMode=0;
                flushCache();
            }
        }
    }

    /** Collects batches into the cache, only rebuilding render data once the last batch arrives */
    public static void onBuildLayout(BuildLayoutMessage message) {
        Minecraft.getInstance().execute(() -> {
            if (message.getBatchIndex() == 0) {
                if (!message.getBuildId().equals(buildId)) orientation = BuildOrientation.IDENTITY;
                buildId = message.getBuildId();
                cache.clear();
            }

            message.getBlocks().forEach((state, positions) ->
                cache.computeIfAbsent(state, s -> new ArrayList<>()).addAll(positions));

            if (!message.hasMore()) refreshBlocks();
        });
    }

    /** Flushes the preview block cache for builds, recent builds are kept**/
    private static void flushCache() {
        cache.clear();
        buildId = "";
        blocks = Map.of();
        visibleBlocks = List.of();
    }

    private static void refreshBlocks() {
        blocks = BuildLayout.fromStateMap(cache).getBlocks(orientation);

        // Only blocks with an exposed face need rendering
        List<BlockPos> visible = new ArrayList<>();
        int maxY = 0;
        for (BlockPos pos : blocks.keySet()) {
            maxY = Math.max(maxY, pos.getY());
            for (Direction dir : Direction.values()) {
                if (!blocks.containsKey(pos.relative(dir))) {
                    visible.add(pos);
                    break;
                }
            }
        }
        visibleBlocks = visible;
        buildHeight = maxY + 1;
    }

    //** Input
    private static final int _ESC = InputConstants.KEY_ESCAPE;
    private static final int _Q = InputConstants.KEY_Q;
    private static final int _E = InputConstants.KEY_E;
    private static final int _Z = InputConstants.KEY_Z;
    private static final int _R = InputConstants.KEY_R;

    private static final int _PRESS = InputConstants.PRESS;
    private static final int _RELEASE = InputConstants.RELEASE;

    private static void onKeyInput(KeyInputEvent event)
    {
        int key = event.getKey();
        if(key == _ESC && event.getAction() == _PRESS) {
            onEscape(); return;
        }

        if(event.getAction() == _RELEASE || !isActive()) return;
        Minecraft mc = Minecraft.getInstance();

        if (event.getKey() == _Q) {
            while (mc.options.keyDrop.consumeClick()) {}
            if (event.getAction() == _PRESS) turn(Direction.Axis.Y);
        } else if (event.getKey() == _E) {
            while (mc.options.keyInventory.consumeClick()) {}
            if (event.getAction() == _PRESS && canTip()) turn(mc.player.getDirection().getAxis());
        } else if (key == _Z && event.getAction() == _PRESS) {
            SimpleStringMessage.createAndFire(mc.player, BuildManager.MSG_UNDO_BUILD, "");
        } else if (key == _R && event.getAction() == _PRESS) {
            SimpleStringMessage.createAndFire(mc.player, BuildManager.MSG_REDO_BUILD, "");
        }
    }

    // Vanilla opens the pause menu before Balm fires, so a PauseScreen here means escape was pressed in game
    private static boolean onEscape()
    {
        if(buildMode==0) return false;
        Minecraft mc = Minecraft.getInstance();
        if(!BuildersAlmanacItem.isHeld(mc.player)) return false;

        mc.setScreen(null);
        buildMode = 0;
        flushCache();
        SimpleStringMessage.createAndFire(mc.player, BuildManager.MSG_EXIT_BUILD, "");
        return true;
    }

    // Houses and supports only turn about Y so their stairs and doors stay upright
    private static boolean canTip() {
        AlmanacEntry entry = AlmanacCatalog.getEntry(buildId);
        return entry != null && entry.category().tips;
    }

    // Q turns about Y, E tips over the horizontal axis the player is facing along
    private static void turn(Direction.Axis axis) {
        orientation = orientation.turn(axis);
        refreshBlocks();
    }

    /** Called from MixinMouseHandler, returns true to stop the hotbar from scrolling */
    public static boolean onMouseScroll(double delta) {
        if (!isActive()) return false;
        if(buildMode==1)
            distance = Mth.clamp(distance + (delta > 0 ? 1 : -1), MIN_DISTANCE, MAX_DISTANCE);
        else if(buildMode==2)
            height = Mth.clamp(height + (delta > 0 ? -1 : 1), -MAX_DISTANCE, MAX_DISTANCE);

        return true;
    }

    public static boolean onMiddleClick() {
        if (!isActive()) return false;
        buildMode++;
        if(buildMode>2) buildMode=1;
        //distance = DEFAULT_DISTANCE; height = DEFAULT_HEIGHT;
        return true;
    }

    /** Called from MixinMinecraftAttack, returns true to cancel the vanilla attack */
    public static boolean onAttack() {
        if (!isActive()) return false;
        LocalPlayer player = Minecraft.getInstance().player;
        BlockPos origin = findOrigin(player);

        JsonObject json = new JsonObject();
        json.addProperty("x", origin.getX());
        json.addProperty("y", origin.getY());
        json.addProperty("z", origin.getZ());
        json.add("orientation", orientation.toJson());
        SimpleStringMessage.createAndFire(player, BuildManager.MSG_PLACE_BUILD, json.toString());

        player.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    //Place structure where the last air block is before it hits the ground,
    //but also we want to be able to attach prefabs to ceilings
    private static BlockPos findOrigin(LocalPlayer player)
    {
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        double x = player.getX() - Mth.sin(yaw) * distance;
        double z = player.getZ() + Mth.cos(yaw) * distance;
        Level level = player.level();

        // close mode, lets player control height with scroll wheel
        if(buildMode==2) {
            return BlockPos.containing(x, player.getEyeY() + height, z);
        }

        BlockPos.MutableBlockPos pos = BlockPos.containing(x, player.getEyeY(), z).mutable();
        while (!level.getBlockState(pos).isAir() && pos.getY() < level.getMaxBuildHeight())
            pos.move(Direction.UP);
        BlockPos start = pos.immutable();

        int bottomCount = 0;
        while (level.getBlockState(pos.below()).isAir() && pos.getY() > level.getMinBuildHeight()) {
            pos.move(Direction.DOWN);
            bottomCount++;
        }
        BlockPos ground = pos.immutable();

        // Only a ceiling closer than the ground matters, so the search stops at bottomCount
        pos.set(start);
        int topCount = 0;
        while (topCount < bottomCount && level.getBlockState(pos.above()).isAir() && pos.getY() < level.getMaxBuildHeight()) {
            pos.move(Direction.UP);
            topCount++;
        }

        boolean ceilingCloser = topCount < bottomCount && !level.getBlockState(pos.above()).isAir();
        if (ceilingCloser)
            return pos.below(buildHeight - 1);

        return ground;
    }

    //** Rendering

    private static void onGuiDraw(GuiDrawEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getElement() != GuiDrawEvent.Element.ALL || buildMode==0 || mc.player == null) return;
        if (mc.options.hideGui || mc.options.renderDebug || !BuildersAlmanacItem.isHeld(mc.player)) return;

        GuiGraphics graphics = event.getGuiGraphics();
        for (int i = 0; i < OVERLAY_LINES.length; i++) {
            Component line = Component.literal("- ").append(Component.translatable("almanac." + Constants.MOD_ID + ".overlay." + OVERLAY_LINES[i]));
            int y = 4 + i * 18;
            graphics.fill(2, y - 1, 26 + mc.font.width(line), y + 17, 0x80000000);
            graphics.renderItem(OVERLAY_ICONS[i], 4, y);
            graphics.drawString(mc.font, line, 23, y + 4, 0xFFFFFF, false);
        }
    }

    private static void onRenderLevel(RenderLevelEvent event) {
        if (!isActive()) return;
        Minecraft mc = Minecraft.getInstance();
        BlockPos origin = findOrigin(mc.player);
        Vec3 cam = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();

        for (BlockPos rel : visibleBlocks) {
            BlockPos pos = origin.offset(rel);
            poseStack.pushPose();
            poseStack.translate(pos.getX() - cam.x, pos.getY() - cam.y, pos.getZ() - cam.z);

            // Air gets a tinted ghost block, occupied spots get a red outline since they will be skipped
            if (mc.level.getBlockState(pos).isAir()) {
                BlockState state = blocks.get(rel);
                dispatcher.getModelRenderer().renderModel(poseStack.last(), buffers.getBuffer(RenderType.translucent()),
                    state, dispatcher.getBlockModel(state), 0.6F, 0.8F, 1.0F, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            } else {
                LevelRenderer.renderLineBox(poseStack, buffers.getBuffer(RenderType.lines()), 0, 0, 0, 1, 1, 1, 1.0F, 0.25F, 0.25F, 1.0F);
            }
            poseStack.popPose();
        }

        buffers.endBatch(RenderType.translucent());
        buffers.endBatch(RenderType.lines());
    }


}
