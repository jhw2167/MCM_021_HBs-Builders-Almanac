package com.holybuckets.builder.core;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.holybuckets.builder.Constants;
import com.holybuckets.builder.LoggerProject;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.event.EventRegistrar;
import net.blay09.mods.balm.api.event.server.ServerStartedEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Dev tool converting The Mighty Architect design JSON in config/hbs_builderalmanac/mighty into BuildRole .nbt files and one metadata JSON in the world's generated folder.
 */
public class MightyDesignConverter {

    public static final String CLASS_ID = "003";
    public static final Path INPUT_DIR = Paths.get("config", Constants.MOD_ID, "mighty");
    public static final String ID_PREFIX = "almanac/mighty/";
    public static final String METADATA_FILE = "almanac_templates.json";

    // Mighty palette letters A-Q, null becomes air
    private static final BuildRole[] LETTER_ROLES = {
        BuildRole.PRIMARY,      // A heavy primary
        BuildRole.PRIMARY,      // B heavy secondary
        null,                   // C heavy window
        BuildRole.QUATERNARY,   // D heavy post
        BuildRole.SECONDARY,    // E inner primary
        BuildRole.SECONDARY,    // F inner secondary
        null,                   // G inner detail
        null,                   // H window
        BuildRole.PRIMARY,      // I outer thick
        BuildRole.QUATERNARY,   // J outer thin
        BuildRole.TERTIARY,     // K outer slab
        BuildRole.TERTIARY,     // L outer flat
        BuildRole.PRIMARY,      // M roof primary
        BuildRole.SECONDARY,    // N floor
        null,                   // O roof detail
        BuildRole.TERTIARY,     // P roof slab
        null                    // Q clear
    };

    public static void init(EventRegistrar reg) {
        reg.registerOnServerStarted(MightyDesignConverter::onServerStarted);
    }

    private static void onServerStarted(ServerStartedEvent event) {
        if (Files.isDirectory(INPUT_DIR)) convertAll(event.getServer());
    }

    public static void convertAll(MinecraftServer server) {
        StructureTemplateManager structures = server.getStructureManager();
        JsonObject metadata = new JsonObject();
        int converted = 0;

        List<Path> files;
        try (Stream<Path> walk = Files.walk(INPUT_DIR)) {
            Set<Path> found = new LinkedHashSet<>();
            for (Path path : walk.toList()) {
                found.add(path);
                if (!Files.isDirectory(path)) continue;
                try (Stream<Path> children = Files.list(path)) {
                    children.forEach(found::add);
                }
            }
            files = found.stream()
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().startsWith("design") && p.toString().endsWith(".json"))
                .toList();
        } catch (Exception e) {
            LoggerProject.logError(CLASS_ID + "001", "Could not list Mighty designs in " + INPUT_DIR.toAbsolutePath() + ": " + e.getMessage());
            return;
        }

        for (Path file : files) {
            String key = INPUT_DIR.relativize(file).toString().replace('\\', '/').replace(".json", "").toLowerCase();
            try {
                JsonObject design = HBUtil.NetworkUtil.jsonToString(Files.readString(file)).getAsJsonObject();
                Set<BuildRole> usedRoles = EnumSet.noneOf(BuildRole.class);
                CompoundTag tag = toStructureTag(design, usedRoles);
                if (usedRoles.isEmpty()) continue;

                ResourceLocation id = new ResourceLocation(Constants.MOD_ID, ID_PREFIX + key);
                structures.getOrCreate(id).load(BuiltInRegistries.BLOCK.asLookup(), tag);
                structures.save(id);

                metadata.add(key, toMetadata(id, tag, usedRoles));
                converted++;
            } catch (Exception e) {
                LoggerProject.logError(CLASS_ID + "002", "Could not convert Mighty design " + file + ": " + e.getMessage());
            }
        }

        Path metadataFile = server.getWorldPath(LevelResource.GENERATED_DIR).resolve(Constants.MOD_ID).resolve(METADATA_FILE);
        try {
            Files.createDirectories(metadataFile.getParent());
        } catch (Exception e) {
            LoggerProject.logError(CLASS_ID + "003", "Could not create " + metadataFile.getParent() + ": " + e.getMessage());
            return;
        }
        HBUtil.FileIO.serializeJsonConfigs(metadataFile.toFile(), new GsonBuilder().setPrettyPrinting().create().toJson(metadata));
        LoggerProject.logInfo(CLASS_ID + "004", "Converted " + converted + " Mighty designs, metadata written to " + metadataFile.toAbsolutePath());
    }

    /** Builds a vanilla structure tag, one block per non-air letter with slabs keeping their top or bottom half */
    private static CompoundTag toStructureTag(JsonObject design, Set<BuildRole> usedRoles) {
        JsonArray layers = design.getAsJsonArray("Layers");
        JsonObject size = design.getAsJsonObject("Size");
        List<BlockState> palette = new ArrayList<>();
        ListTag blocks = new ListTag();

        for (int y = 0; y < layers.size(); y++) {
            JsonObject layer = layers.get(y).getAsJsonObject();
            String[] rows = layer.get("Blocks").getAsString().split(",");
            String[] facings = layer.has("Facing") ? layer.get("Facing").getAsString().split(",") : null;

            for (int z = 0; z < rows.length; z++) {
                for (int x = 0; x < rows[z].length(); x++) {
                    int letter = rows[z].charAt(x) - 'A';
                    if (letter < 0 || letter >= LETTER_ROLES.length || LETTER_ROLES[letter] == null) continue;

                    BuildRole role = LETTER_ROLES[letter];
                    BlockState state = role.block.defaultBlockState();
                    if (facings != null && state.hasProperty(SlabBlock.TYPE) && isTopHalf(facings[z].charAt(x)))
                        state = state.setValue(SlabBlock.TYPE, SlabType.TOP);

                    int index = palette.indexOf(state);
                    if (index < 0) {
                        palette.add(state);
                        index = palette.size() - 1;
                    }

                    CompoundTag block = new CompoundTag();
                    block.put("pos", intList(x, y, z));
                    block.putInt("state", index);
                    blocks.add(block);
                    usedRoles.add(role);
                }
            }
        }

        ListTag paletteTag = new ListTag();
        for (BlockState state : palette) paletteTag.add(NbtUtils.writeBlockState(state));

        CompoundTag tag = new CompoundTag();
        tag.put("size", intList(size.get("X").getAsInt(), layers.size(), size.get("Z").getAsInt()));
        tag.put("palette", paletteTag);
        tag.put("blocks", blocks);
        tag.put("entities", new ListTag());
        return NbtUtils.addCurrentDataVersion(tag);
    }

    /** Mighty orientation letters B (top) and J-O (top with a facing) are upper halves */
    private static boolean isTopHalf(char facing) {
        int orientation = facing - 'A';
        return orientation == 1 || (orientation >= 9 && orientation <= 14);
    }

    /** One slot entry per BuildRole in order, null when the template does not use that role */
    private static JsonObject toMetadata(ResourceLocation id, CompoundTag tag, Set<BuildRole> usedRoles) {
        ListTag size = tag.getList("size", Tag.TAG_INT);
        JsonArray sizeJson = new JsonArray();
        for (int i = 0; i < size.size(); i++) sizeJson.add(size.getInt(i));

        JsonArray slots = new JsonArray();
        for (BuildRole role : BuildRole.values()) slots.add(usedRoles.contains(role) ? role.slotType : null);

        JsonObject json = new JsonObject();
        json.addProperty("id", id.toString());
        json.add("size", sizeJson);
        json.addProperty("blocks", tag.getList("blocks", Tag.TAG_COMPOUND).size());
        json.add("slots", slots);
        return json;
    }

    private static ListTag intList(int... values) {
        ListTag list = new ListTag();
        for (int value : values) list.add(IntTag.valueOf(value));
        return list;
    }
}
