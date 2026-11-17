package com.holybuckets.builder.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.holybuckets.builder.LoggerProject;
import com.holybuckets.builder.core.AlmanacTemplates;
import com.holybuckets.foundation.client.ClientEventRegistrar;
import com.holybuckets.foundation.event.custom.SimpleMessageEvent;

import javax.annotation.Nullable;

public class ClientAlmanacTemplates {

    public static final String CLASS_ID = "005";

    private static volatile String selectedEntryId = null;
    private static volatile JsonObject selectedTemplate = null;

    public static void init(ClientEventRegistrar reg) {
        reg.registerOnSimpleMessage(AlmanacTemplates.MSG_SELECTED_TEMPLATE, ClientAlmanacTemplates::onSelectedTemplate);
        reg.registerOnDisconnectedFromServer(e -> clear());
    }

    @Nullable
    public static String getSelectedEntryId() {
        return selectedEntryId;
    }

    @Nullable
    public static JsonObject getSelectedTemplate() {
        return selectedTemplate;
    }

    private static void onSelectedTemplate(SimpleMessageEvent event) {
        try {
            JsonObject payload = JsonParser.parseString(event.getContent()).getAsJsonObject();
            String entryId = payload.has(AlmanacTemplates.KEY_ENTRY) ? payload.get(AlmanacTemplates.KEY_ENTRY).getAsString() : "";
            JsonObject template = payload.has(AlmanacTemplates.KEY_TEMPLATE) ? payload.getAsJsonObject(AlmanacTemplates.KEY_TEMPLATE) : null;
            selectedEntryId = entryId.isEmpty() ? null : entryId;
            selectedTemplate = template;
        } catch (RuntimeException e) {
            LoggerProject.logError(CLASS_ID + "001", "Could not read selected almanac template: " + e.getMessage());
        }
    }

    private static void clear() {
        selectedEntryId = null;
        selectedTemplate = null;
    }
}
