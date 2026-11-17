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
 * Every build in the almanac, stored as rows of up to 9 entries per category with empty rows between sections.
 * Null entries leave a slot empty.
 */
public class AlmanacCatalog {

    public static final int COLUMNS = 9;
    public static final String HOLLOW = "hollow_";
    private static final int[] SIZES = {3, 6, 9, 12};
    private static final int[] SINGLE = {0};
    private static final int[] WALL_HEIGHTS = {2, 8, 14, 20};
    private static final Map<AlmanacCategory, List<List<AlmanacEntry>>> ROWS = new EnumMap<>(AlmanacCategory.class);
    private static final Map<String, AlmanacEntry> BY_ID = new HashMap<>();

    static {
        // Planes: columns step x, rows step z, outlines to the right
        addShape(BASIC_SHAPES, "circle", Items.SANDSTONE_SLAB, Items.SANDSTONE_WALL, SINGLE, false);
        addShape(BASIC_SHAPES, "semicircle", Items.CHISELED_SANDSTONE , Items.SANDSTONE_WALL, SINGLE, false);
        addShape(BASIC_SHAPES, "ellipse", Items.RED_SANDSTONE_SLAB, Items.RED_SANDSTONE_WALL, SIZES, false);
        addShape(BASIC_SHAPES, "square", Items.SMOOTH_STONE_SLAB, Items.STONE_BRICK_WALL, SINGLE, false);
        addShape(BASIC_SHAPES, "rectangle", Items.STONE_SLAB, Items.COBBLESTONE_WALL, SIZES, false);
        addShape(BASIC_SHAPES, "triangle", Items.PRISMARINE_SLAB, Items.PRISMARINE_WALL, SIZES, false);
        addShape(BASIC_SHAPES, "right_triangle", Items.DARK_PRISMARINE_SLAB, Items.PRISMARINE_WALL, SIZES, false);
        addShape(BASIC_SHAPES, "trapezoid", Items.BRICK_SLAB, Items.BRICK_WALL, SIZES, false);

        // Solids: columns step width, rows step height, hollow shells to the right
        addShape(BASIC_3D, "sphere", Items.WHITE_CONCRETE, Items.TINTED_GLASS, SINGLE, true);
        addShape(BASIC_3D, "half_sphere", Items.LIGHT_GRAY_CONCRETE, Items.WHITE_STAINED_GLASS, SINGLE, true);
        addShape(BASIC_3D, "cube", Items.STONE_BRICKS, Items.GLASS, SIZES, true);
        addShape(BASIC_3D, "prism", Items.PRISMARINE, Items.CYAN_STAINED_GLASS, SIZES, true);
        addShape(BASIC_3D, "cylinder", Items.QUARTZ_PILLAR, Items.LIGHT_BLUE_STAINED_GLASS, SIZES, true);
        addShape(BASIC_3D, "cone", Items.ORANGE_TERRACOTTA, Items.ORANGE_STAINED_GLASS, SIZES, true);
        addShape(BASIC_3D, "truncated_cone", Items.BRICKS, Items.BROWN_STAINED_GLASS, SIZES, true);

        addShape(FRAME_3D, "sphere", Items.CHAIN, null, SINGLE, true);
        addShape(FRAME_3D, "half_sphere", Items.END_ROD, null, SINGLE, true);
        addShape(FRAME_3D, "cube", Items.IRON_BARS, null, SIZES, true);
        addShape(FRAME_3D, "prism", Items.PRISMARINE_WALL, null, SIZES, true);
        addShape(FRAME_3D, "cylinder", Items.OAK_FENCE, null, SIZES, true);
        addShape(FRAME_3D, "cone", Items.SPRUCE_FENCE, null, SIZES, true);
        addShape(FRAME_3D, "truncated_cone", Items.NETHER_BRICK_FENCE, null, SIZES, true);

        // Walls: columns step length, rows step height
        addShape(WALLS, "single_wall", Items.COBBLESTONE_WALL, null, WALL_HEIGHTS, true);
        addShape(WALLS, "double_wall", Items.STONE_BRICKS, null, WALL_HEIGHTS, true);
        addShape(WALLS, "triple_wall", Items.POLISHED_ANDESITE, null, WALL_HEIGHTS, true);
        addShape(WALLS, "crenellated_wall", Items.DEEPSLATE_BRICK_WALL, null, WALL_HEIGHTS, true);
        addShape(WALLS, "corner_wall", Items.MOSSY_COBBLESTONE_WALL, null, WALL_HEIGHTS, true);
        addShape(WALLS, "square_enclosure", Items.BRICK_WALL, null, WALL_HEIGHTS, true);
        addShape(WALLS, "round_enclosure", Items.STONE_BRICK_WALL, null, WALL_HEIGHTS, true);

        addSection(HOUSES, List.of(List.of(
            build(HOUSES, "small_hut", Items.OAK_PLANKS),
            build(HOUSES, "log_cabin", Items.SPRUCE_LOG),
            build(HOUSES, "cottage", Items.COBBLESTONE),
            build(HOUSES, "farmhouse", Items.HAY_BLOCK),
            build(HOUSES, "longhouse", Items.DARK_OAK_PLANKS),
            build(HOUSES, "tower_house", Items.STONE_BRICKS)
        )));

        addSection(SUPPORT_STRUCTURES, List.of(List.of(
            build(SUPPORT_STRUCTURES, "pillar", Items.OAK_LOG),
            build(SUPPORT_STRUCTURES, "beam", Items.STRIPPED_OAK_LOG),
            build(SUPPORT_STRUCTURES, "arch", Items.STONE_BRICK_STAIRS),
            build(SUPPORT_STRUCTURES, "buttress", Items.COBBLESTONE_WALL),
            build(SUPPORT_STRUCTURES, "bridge_span", Items.OAK_SLAB),
            build(SUPPORT_STRUCTURES, "scaffold_tower", Items.SCAFFOLDING),
            build(SUPPORT_STRUCTURES, "staircase", Items.OAK_STAIRS)
        )));

        List<List<AlmanacEntry>> medievalRoofs = new ArrayList<>();
        medievalRoofs.addAll(designRows(ROOFS, "medieval", 14, Items.SPRUCE_STAIRS));
        medievalRoofs.addAll(designRows(ROOFS, "medieval-flatroof", 7, Items.SPRUCE_SLAB));
        medievalRoofs.addAll(designRows(ROOFS, "medieval-towerroof", 7, Items.DARK_OAK_STAIRS));
        medievalRoofs.addAll(designRows(ROOFS, "medieval-towerflatroof", 7, Items.STONE_BRICK_SLAB));
        addSection(ROOFS, medievalRoofs);

        List<List<AlmanacEntry>> modernRoofs = new ArrayList<>();
        modernRoofs.addAll(designRows(ROOFS, "modern", 5, Items.SMOOTH_QUARTZ_SLAB));
        modernRoofs.addAll(designRows(ROOFS, "townhouse", 11, Items.BRICK_STAIRS));
        modernRoofs.addAll(designRows(ROOFS, "townhouse-towerroof", 10, Items.DEEPSLATE_TILE_STAIRS));
        addSection(ROOFS, modernRoofs);

        addSection(SIMPLE_HOUSES, List.of(List.of(
            build(SIMPLE_HOUSES, "simple_house_1", Items.STONE_BRICKS),
            build(SIMPLE_HOUSES, "simple_house_2", Items.SANDSTONE)
        )));

        addSection(MEDIEVAL_HOUSES, List.of(List.of(
            build(MEDIEVAL_HOUSES, "medieval_house_1", Items.DIORITE),
            build(MEDIEVAL_HOUSES, "medieval_house_2", Items.STRIPPED_SPRUCE_LOG),
            build(MEDIEVAL_HOUSES, "medieval_house_3", Items.STONE_BRICKS)
        )));

        addSection(MODERN_HOUSES, List.of(List.of(
            build(MODERN_HOUSES, "modern_house_1", Items.WHITE_CONCRETE),
            build(MODERN_HOUSES, "modern_house_2", Items.CYAN_TERRACOTTA)
        )));
    }

    public static List<List<AlmanacEntry>> getRows(AlmanacCategory category) {
        return ROWS.getOrDefault(category, List.of());
    }

    /** Returns null for an unknown id */
    public static AlmanacEntry getEntry(String id) {
        return BY_ID.get(id);
    }

    // Columns step the size, rows step rowSizes (height when vertical, otherwise z), a hollow icon adds hollow variants after an empty column
    private static void addShape(AlmanacCategory category, String name, Item icon, Item hollowIcon, int[] rowSizes, boolean vertical) {
        List<List<AlmanacEntry>> rows = new ArrayList<>();
        for (int second : rowSizes) {
            List<AlmanacEntry> row = new ArrayList<>();
            addEntries(row, category, name, icon, second, vertical);
            if (hollowIcon != null) {
                row.add(null);
                addEntries(row, category, HOLLOW + name, hollowIcon, second, vertical);
            }
            rows.add(row);
        }
        addSection(category, rows);
    }

    private static void addEntries(List<AlmanacEntry> row, AlmanacCategory category, String name, Item icon, int second, boolean vertical) {
        for (int size : SIZES) {
            row.add(new AlmanacEntry(category, name, size, vertical ? second : 0, vertical ? 0 : second, icon));
        }
    }

    // Sections after the first are separated by an empty row
    private static void addSection(AlmanacCategory category, List<List<AlmanacEntry>> rows) {
        List<List<AlmanacEntry>> categoryRows = ROWS.computeIfAbsent(category, c -> new ArrayList<>());
        if (!categoryRows.isEmpty()) categoryRows.add(List.of());
        for (List<AlmanacEntry> row : rows) {
            categoryRows.add(row);
            for (AlmanacEntry entry : row) {
                if (entry != null) BY_ID.put(entry.getId(), entry);
            }
        }
    }

    private static List<List<AlmanacEntry>> designRows(AlmanacCategory category, String folder, int count, Item icon) {
        List<List<AlmanacEntry>> rows = new ArrayList<>();
        List<AlmanacEntry> row = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            if (row.size() == COLUMNS) {
                rows.add(row);
                row = new ArrayList<>();
            }
            row.add(build(category, folder + "-" + i, icon));
        }
        if (!row.isEmpty()) rows.add(row);
        return rows;
    }

    private static AlmanacEntry build(AlmanacCategory category, String name, Item icon) {
        return new AlmanacEntry(category, name, 0, 0, 0, icon);
    }
}
