package com.holybuckets.builder.menu;

import com.holybuckets.builder.almanac.AlmanacCatalog;
import com.holybuckets.builder.almanac.AlmanacCategory;
import com.holybuckets.builder.almanac.AlmanacEntry;
import com.holybuckets.builder.core.BuildManager;
import com.holybuckets.builder.item.BuildIconItem;
import com.holybuckets.builder.item.BuildersAlmanacItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static com.holybuckets.builder.almanac.AlmanacCatalog.COLUMNS;

/**
 * Creative Menu-esque selection menu. Each "item" in this menu represents a type of build
 * the tabs sort different builds into categories.
 *
 * Selecting a tab demarcates the selected build type and exit the menu.
 * The menu can be exited with E, Esc, or selecting a build.
 */
public class AlmanacMenu extends AbstractContainerMenu {

    public static final int GRID_ROWS = 5;
    public static final int GRID_SIZE = GRID_ROWS * COLUMNS;
    public static final int SCROLL_BUTTON_OFFSET = 10;

    private final InteractionHand hand;
    private final SimpleContainer grid = new SimpleContainer(GRID_SIZE);
    private final AlmanacEntry[] gridEntries = new AlmanacEntry[GRID_SIZE];

    private final SimpleContainer blockPanel = new SimpleContainer(4);

    private AlmanacCategory tab = AlmanacCategory.BASIC_SHAPES;
    private int rowOffset = 0;
    private AlmanacEntry selected;

    public AlmanacMenu(int syncId, Inventory playerInventory, InteractionHand hand, AlmanacCategory tab)
    {
        super(ModMenus.almanacMenu.get(), syncId);
        this.hand = hand;
        if (tab != null) this.tab = tab;

        // Build grid (slots 0-44)
        for (int row = 0; row < GRID_ROWS; ++row) {
            for (int col = 0; col < COLUMNS; ++col) {
                this.addSlot(new LockedSlot(grid, row * COLUMNS + col, 9 + col * 18, 18 + row * 18));
            }
        }

        // Hotbar, the slot holding the almanac is locked
        for (int col = 0; col < COLUMNS; ++col) {
            if (hand == InteractionHand.MAIN_HAND && col == playerInventory.selected)
                this.addSlot(new LockedSlot(playerInventory, col, 9 + col * 18, 112));
            else
                this.addSlot(new Slot(playerInventory, col, 9 + col * 18, 112));
        }

        // add the blockPanel negative left aligned
        for(int i = 0; i < 4; i++) {
            this.addSlot(new Slot(blockPanel, i, -20, 18 + i * 18));
        }

        refreshGrid();
    }

    public AlmanacCategory getTab() { return tab; }
    public int getRowOffset() { return rowOffset; }
    public AlmanacEntry getSelected() { return selected; }
    public AlmanacEntry getEntry(int slot) { return gridEntries[slot]; }

    public int getMaxRowOffset() {
        return Math.max(0, AlmanacCatalog.getRows(tab).size() - GRID_ROWS);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0) return false;

        if (id < SCROLL_BUTTON_OFFSET) {
            if (id >= AlmanacCategory.values().length) return false;
            tab = AlmanacCategory.values()[id];
            rowOffset = 0;
            if (player instanceof ServerPlayer sp) BuildManager.get(sp).setLastTab(tab);
        } else {
            rowOffset = Mth.clamp(id - SCROLL_BUTTON_OFFSET, 0, getMaxRowOffset());
        }

        refreshGrid();
        return true;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < GRID_SIZE) {
            if (gridEntries[slotId] == null) return;
            selected = gridEntries[slotId];
            if (player instanceof ServerPlayer sp) {
                BuildManager.get(sp).setSelected(sp, selected);
                sp.closeContainer();
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    private void refreshGrid()
    {
        List<List<AlmanacEntry>> entries = AlmanacCatalog.getRows(tab);
        for (int row = 0; row < GRID_ROWS; row++)
        {
            int i = row + rowOffset;
            List<AlmanacEntry> rowEntries = (i < entries.size()) ? entries.get(i) : List.of();
            for(int col = 0; col < COLUMNS; col++)
            {
                AlmanacEntry entry = null;
                if (col < rowEntries.size()) entry = rowEntries.get(col);

                int rowCol = row * COLUMNS + col;
                gridEntries[rowCol] = entry;

                grid.setItem(rowCol, BuildIconItem.of(entry));
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(hand).getItem() instanceof BuildersAlmanacItem;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }


    // Slots are only for menu selection, not typical item inventory slots
    private static class LockedSlot extends Slot {
        public LockedSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }
}
