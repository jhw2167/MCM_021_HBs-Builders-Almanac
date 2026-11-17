package com.holybuckets.builder.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.holybuckets.builder.Constants;
import com.holybuckets.builder.LoggerProject;
import com.holybuckets.builder.almanac.AlmanacEntry;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.networking.SimpleStringMessage;
import net.blay09.mods.balm.api.event.server.ServerStartedEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;

import javax.annotation.Nullable;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AlmanacTemplates {

    public static final String CLASS_ID = "004";
    public static final String MSG_SELECTED_TEMPLATE = Constants.MOD_ID + ":selected_template";
    public static final String KEY_ENTRY = "entry";
    public static final String KEY_TEMPLATE = "template";
    public static final ResourceLocation FILE = new ResourceLocation(Constants.MOD_ID, "almanac_templates.json");

    private static final Map<String, JsonObject> BY_KEY = new HashMap<>();
    private static final Map<String, JsonObject> BY_STRUCTURE_ID = new HashMap<>();

    public static void init(EventRegistrar reg) {
        reg.registerOnServerStarted(AlmanacTemplates::onServerStarted);
        reg.registerOnServerStopped(e -> clear());
    }

    @Nullable
    public static JsonObject get(String key) {
        return BY_KEY.get(key);
    }

    @Nullable
    public static JsonObject get(AlmanacEntry entry) {
        if (entry == null) return null;
        return BY_STRUCTURE_ID.get(entry.getStructureId().toString());
    }

    public static void load(MinecraftServer server) {
        clear();

        Optional<Resource> resource = server.getResourceManager().getResource(FILE);
        if (resource.isEmpty()) {
            LoggerProject.logWarning(CLASS_ID + "000", "No almanac template metadata found at data/" + FILE.getNamespace() + "/" + FILE.getPath());
            return;
        }

        JsonObject all;
        try (Reader reader = resource.get().openAsReader()) {
            all = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            LoggerProject.logError(CLASS_ID + "001", "Could not read almanac template metadata " + FILE + ": " + e.getMessage());
            return;
        }

        for (Map.Entry<String, JsonElement> entry : all.entrySet()) {
            if (!entry.getValue().isJsonObject()) continue;
            JsonObject template = entry.getValue().getAsJsonObject();
            BY_KEY.put(entry.getKey(), template);
            if (template.has("id")) BY_STRUCTURE_ID.put(template.get("id").getAsString(), template);
        }
        LoggerProject.logInfo(CLASS_ID + "002", "Loaded " + BY_KEY.size() + " almanac templates");
    }

    public static void sendSelected(ServerPlayer player, @Nullable AlmanacEntry entry) {
        if (player == null) return;

        JsonObject payload = new JsonObject();
        payload.addProperty(KEY_ENTRY, (entry == null) ? "" : entry.getId());
        JsonObject template = get(entry);
        if (template != null) payload.add(KEY_TEMPLATE, template);

        String content = payload.toString();
        if (content.length() > SimpleStringMessage.MAX_SIZE) {
            LoggerProject.logWarning(CLASS_ID + "003", "Almanac template for " + entry.getId() + " is too large to send");
            payload.remove(KEY_TEMPLATE);
            content = payload.toString();
        }
        SimpleStringMessage.createAndFire(player, MSG_SELECTED_TEMPLATE, content);
    }

    private static void clear() {
        BY_KEY.clear();
        BY_STRUCTURE_ID.clear();
    }

    private static void onServerStarted(ServerStartedEvent event) {
        load(event.getServer());
    }
}
