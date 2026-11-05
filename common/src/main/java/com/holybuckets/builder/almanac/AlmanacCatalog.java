package com.holybuckets.builder.almanac;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.holybuckets.builder.almanac.AlmanacCategory.*;

/**
 * Every build in the almanac, stored as rows of up to 9 entries per category.
 */
public class AlmanacCatalog {

    public static final int COLUMNS = 9;
    private static final int[] SHAPE_SIZES = {3, 5, 7, 9, 11, 13, 15, 17, 19};
    private static final Map<AlmanacCategory, List<List<AlmanacEntry>>> ROWS = new EnumMap<>(AlmanacCategory.class);
    private static final Map<String, AlmanacEntry> BY_ID = new HashMap<>();

    static {
        addShapeRow("square_plane", Items.SMOOTH_STONE_SLAB);
        addShapeRow("cube", Items.STONE_BRICKS);
        addShapeRow("circle_plane", Items.SANDSTONE_SLAB);
        addShapeRow("hollow_cube", Items.GLASS);
        addShapeRow("sphere", Items.WHITE_CONCRETE);
        addShapeRow("half_sphere", Items.LIGHT_GRAY_CONCRETE);
        addShapeRow("hollow_sphere", Items.TINTED_GLASS);

        addRow(HOUSES, List.of(
            build(HOUSES, "small_hut", Items.OAK_PLANKS),
            build(HOUSES, "log_cabin", Items.SPRUCE_LOG),
            build(HOUSES, "cottage", Items.COBBLESTONE),
            build(HOUSES, "farmhouse", Items.HAY_BLOCK),
            build(HOUSES, "longhouse", Items.DARK_OAK_PLANKS),
            build(HOUSES, "tower_house", Items.STONE_BRICKS)
        ));

        addRow(SUPPORT_STRUCTURES, List.of(
            build(SUPPORT_STRUCTURES, "pillar", Items.OAK_LOG),
            build(SUPPORT_STRUCTURES, "beam", Items.STRIPPED_OAK_LOG),
            build(SUPPORT_STRUCTURES, "arch", Items.STONE_BRICK_STAIRS),
            build(SUPPORT_STRUCTURES, "buttress", Items.COBBLESTONE_WALL),
            build(SUPPORT_STRUCTURES, "bridge_span", Items.OAK_SLAB),
            build(SUPPORT_STRUCTURES, "scaffold_tower", Items.SCAFFOLDING),
            build(SUPPORT_STRUCTURES, "staircase", Items.OAK_STAIRS)
        ));
    }

    public static List<List<AlmanacEntry>> getRows(AlmanacCategory category) {
        return ROWS.getOrDefault(category, List.of());
    }

    /** Returns null for an unknown id */
    public static AlmanacEntry getEntry(String id) {
        return BY_ID.get(id);
    }

    private static void addShapeRow(String name, Item icon) {
        List<AlmanacEntry> row = new ArrayList<>();
        for (int size : SHAPE_SIZES) {
            row.add(new AlmanacEntry(BASIC_SHAPES, name, size, icon));
        }
        addRow(BASIC_SHAPES, row);
    }

    private static void addRow(AlmanacCategory category, List<AlmanacEntry> row) {
        ROWS.computeIfAbsent(category, c -> new ArrayList<>()).add(row);
        for (AlmanacEntry entry : row) BY_ID.put(entry.getId(), entry);
    }

    private static AlmanacEntry build(AlmanacCategory category, String name, Item icon) {
        return new AlmanacEntry(category, name, 0, icon);
    }
}
