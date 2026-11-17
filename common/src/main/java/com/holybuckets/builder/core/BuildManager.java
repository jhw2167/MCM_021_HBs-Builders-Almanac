package com.holybuckets.builder.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.holybuckets.builder.Constants;
import com.holybuckets.builder.LoggerProject;
import com.holybuckets.builder.almanac.AlmanacCategory;
import com.holybuckets.builder.almanac.AlmanacEntry;
import com.holybuckets.builder.item.BuildersAlmanacItem;
import com.holybuckets.builder.networking.BuildLayoutMessage;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.ServerTickEvent;
import com.holybuckets.foundation.event.custom.SimpleMessageEvent;
import com.holybuckets.foundation.event.custom.TickType;
import com.holybuckets.foundation.networking.SimpleStringMessage;
import net.blay09.mods.balm.api.event.PlayerLogoutEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Server side build state, one per player. Held in RAM only, so selections and
 * placement history are dropped when the server stops.
 */
public class BuildManager {

    public static final String CLASS_ID = "002";
    public static final String MSG_BUILD_MODE = Constants.MOD_ID + ":build_mode";   // server -> client
    public static final String MSG_PLACE_BUILD = Constants.MOD_ID + ":place_build"; // client -> server
    public static final String MSG_EXIT_BUILD = Constants.MOD_ID + ":exit_build";   // client -> server
    public static final String MSG_UNDO_BUILD = Constants.MOD_ID + ":undo_build";   // client -> server
    public static final String MSG_REDO_BUILD = Constants.MOD_ID + ":redo_build";   // client -> server
    private static final int MAX_PLACE_DISTANCE = 64;

    private static final Map<String, BuildManager> MANAGERS = new HashMap<>();

    private final String playerId;
    private boolean buildMode = false;
    private AlmanacEntry selected;
    private AlmanacCategory lastTab = AlmanacCategory.BASIC_SHAPES;
    private final BlockState[] previewBlocks = new BlockState[BuildRole.values().length];
    private final List<AlmanacEntry> buildHistory = new ArrayList<>();
    private final List<PlacedBuild> placedHistory = new ArrayList<>();
    private int historyIndex = 0;

    public record PlacedBuild(AlmanacEntry entry, ResourceKey<Level> dimension, BlockPos origin, BuildOrientation orientation, Map<BlockPos, BlockState> placedBlocks) {}

    private BuildManager(String playerId) {
        this.playerId = playerId;
        for (BuildRole role : BuildRole.values()) {
            previewBlocks[role.ordinal()] = role.block.defaultBlockState();
        }
    }

    public static void init(EventRegistrar reg) {
        reg.registerOnServerTick(TickType.ON_20_TICKS, BuildManager::on20Ticks);
        reg.registerOnSimpleMessage(MSG_PLACE_BUILD, BuildManager::onPlaceBuildMessage);
        reg.registerOnSimpleMessage(MSG_EXIT_BUILD, BuildManager::onExitBuildMessage);
        reg.registerOnSimpleMessage(MSG_UNDO_BUILD, BuildManager::onUndoBuildMessage);
        reg.registerOnSimpleMessage(MSG_REDO_BUILD, BuildManager::onRedoBuildMessage);
        reg.registerOnPlayerLogout(BuildManager::onPlayerLogout);
        reg.registerOnServerStopped(e -> MANAGERS.clear());
    }

    public static BuildManager get(Player player) {
        return MANAGERS.computeIfAbsent(HBUtil.PlayerUtil.getId(player), BuildManager::new);
    }

    public boolean isBuildMode() { return buildMode; }
    public AlmanacEntry getSelected() { return selected; }
    public AlmanacCategory getLastTab() { return lastTab; }
    public void setLastTab(AlmanacCategory tab) { if (tab != null) lastTab = tab; }
    public BlockState[] getPreviewBlocks() { return previewBlocks; }
    public List<AlmanacEntry> getBuildHistory() { return buildHistory; }
    public List<PlacedBuild> getPlacedHistory() { return placedHistory; }

    //** Build mode

    public void enterBuildMode(ServerPlayer player) {
        buildMode = true;
        syncToClient(player);
        sendLayout(player);
        AlmanacTemplates.sendSelected(player, selected);
    }

    public void exitBuildMode(ServerPlayer player) {
        buildMode = false;
        syncToClient(player);
    }

    public void setSelected(ServerPlayer player, AlmanacEntry entry) {
        if (entry == selected) return;
        selected = entry;
        buildHistory.add(entry);
        sendLayout(player);
        AlmanacTemplates.sendSelected(player, selected);
    }

    public void setPreviewBlock(ServerPlayer player, BuildRole role, BlockState state) {
        previewBlocks[role.ordinal()] = state;
        sendLayout(player);
    }

    private void syncToClient(ServerPlayer player) {
        JsonObject json = new JsonObject();
        json.addProperty("buildMode", buildMode);
        SimpleStringMessage.createAndFire(player, MSG_BUILD_MODE, json.toString());
    }

    private void sendLayout(ServerPlayer player) {
        if (selected == null) return;
        BuildLayoutMessage.createAndFire(player, selected.getId(), getLayout(player.getServer()), previewBlocks);
    }

    /** Shape tabs are generated, everything else reads data/hbs_builderalmanac/structures/almanac/[id].nbt */
    private BuildLayout getLayout(MinecraftServer server) {
        if (selected.category().generated) return BuildShapes.getLayout(selected);
        return server.getStructureManager().get(selected.getStructureId())
            .map(BuildLayout::fromTemplate)
            .orElse(BuildLayout.EMPTY);
    }

    //** Placement

    private void place(ServerPlayer player, BlockPos origin, BuildOrientation orientation) {
        if (!buildMode || selected == null || !BuildersAlmanacItem.isHeld(player)) return;
        if (!selected.category().tips && !orientation.isUpright()) return;
        if (!origin.closerThan(player.blockPosition(), MAX_PLACE_DISTANCE)) return;

        Level level = player.level();
        Map<BlockPos, BlockState> placed = new LinkedHashMap<>();
        for (Map.Entry<BlockPos, BlockState> block : getLayout(player.getServer()).getBlocks(orientation).entrySet()) {
            BlockPos pos = origin.offset(block.getKey());
            if (!level.getBlockState(pos).isAir()) continue;
            level.setBlock(pos, block.getValue(), Block.UPDATE_ALL);
            placed.put(pos, block.getValue());
        }

        // A new placement discards anything that was undone
        placedHistory.subList(historyIndex, placedHistory.size()).clear();
        placedHistory.add(new PlacedBuild(selected, level.dimension(), origin, orientation, placed));
        historyIndex = placedHistory.size();
        LoggerProject.logDebug(CLASS_ID + "001", "Placed " + selected.getId() + " for " + playerId + " at " + origin + ", " + placed.size() + " blocks");
    }

    private void undo(ServerPlayer player) {
        if (historyIndex == 0 || !BuildersAlmanacItem.isHeld(player)) return;
        PlacedBuild build = placedHistory.get(historyIndex - 1);
        ServerLevel level = player.getServer().getLevel(build.dimension());
        if (level == null) return;
        historyIndex--;

        // UPDATE_CLIENTS skips neighbour updates so attached blocks do not pop off and drop
        build.placedBlocks().forEach((pos, state) -> {
            if (level.getBlockState(pos).is(state.getBlock()))
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        });
        sendHistoryMessage(player, "undone", build);
    }

    private void redo(ServerPlayer player) {
        if (historyIndex >= placedHistory.size() || !BuildersAlmanacItem.isHeld(player)) return;
        PlacedBuild build = placedHistory.get(historyIndex);
        ServerLevel level = player.getServer().getLevel(build.dimension());
        if (level == null) return;
        historyIndex++;

        build.placedBlocks().forEach((pos, state) -> {
            if (level.getBlockState(pos).isAir())
                level.setBlock(pos, state, Block.UPDATE_ALL);
        });
        sendHistoryMessage(player, "redone", build);
    }

    private static void sendHistoryMessage(ServerPlayer player, String key, PlacedBuild build) {
        player.displayClientMessage(Component.translatable("almanac." + Constants.MOD_ID + "." + key,
            build.entry().getDisplayName(), build.origin().toShortString()), true);
    }

    //** Events

    private static void on20Ticks(ServerTickEvent event) {
        for (Map.Entry<String, BuildManager> entry : MANAGERS.entrySet()) {
            BuildManager manager = entry.getValue();
            if (!manager.buildMode) continue;
            Player p = HBUtil.PlayerUtil.getPlayer(entry.getKey(), HBUtil.PlayerUtil.PlayerNameSpace.SERVER);
            if (p instanceof ServerPlayer sp && !BuildersAlmanacItem.isHeld(sp))
                manager.exitBuildMode(sp);
        }
    }

    private static void onPlaceBuildMessage(SimpleMessageEvent event) {
        ServerPlayer player = HBUtil.PlayerUtil.getServerPlayer(event.getPlayer());
        JsonObject json = JsonParser.parseString(event.getContent()).getAsJsonObject();
        BlockPos origin = new BlockPos(json.get("x").getAsInt(), json.get("y").getAsInt(), json.get("z").getAsInt());
        BuildOrientation orientation = BuildOrientation.fromJson(json.get("orientation"));

        // Integrated servers deliver this on the client thread
        player.getServer().execute(() -> get(player).place(player, origin, orientation));
    }

    private static void onExitBuildMessage(SimpleMessageEvent event) {
        ServerPlayer player = HBUtil.PlayerUtil.getServerPlayer(event.getPlayer());
        player.getServer().execute(() -> get(player).exitBuildMode(player));
    }

    private static void onUndoBuildMessage(SimpleMessageEvent event) {
        ServerPlayer player = HBUtil.PlayerUtil.getServerPlayer(event.getPlayer());
        player.getServer().execute(() -> get(player).undo(player));
    }

    private static void onRedoBuildMessage(SimpleMessageEvent event) {
        ServerPlayer player = HBUtil.PlayerUtil.getServerPlayer(event.getPlayer());
        player.getServer().execute(() -> get(player).redo(player));
    }

    private static void onPlayerLogout(PlayerLogoutEvent event) {
        BuildManager manager = MANAGERS.get(HBUtil.PlayerUtil.getId(event.getPlayer()));
        if (manager != null) manager.buildMode = false;
    }
}
