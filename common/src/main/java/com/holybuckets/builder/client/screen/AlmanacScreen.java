package com.holybuckets.builder.client.screen;

import com.holybuckets.builder.almanac.AlmanacCategory;
import com.holybuckets.builder.menu.AlmanacMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Screen for the Builder's Almanac, laid out like the creative inventory using its vanilla textures.
 */
public class AlmanacScreen extends AbstractContainerScreen<AlmanacMenu> {

    private static final ResourceLocation TEXTURE =
        new ResourceLocation("textures/gui/container/creative_inventory/tab_items.png");
    private static final ResourceLocation TABS_TEXTURE =
        new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");

    public static int INV_HEIGHT = 136;
    public static int INV_WIDTH = 195;

    private static final int SCROLL_X = 175;
    private static final int SCROLL_Y = 18;
    private static final int SCROLL_HEIGHT = 112;
    private static final int TAB_WIDTH = 27;
    private static final int SELECTED_COLOR = 0xFFE0B040;

    private boolean scrolling = false;

    public AlmanacScreen(AlmanacMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = INV_HEIGHT;
        this.imageWidth = INV_WIDTH;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        for (AlmanacCategory tab : AlmanacCategory.values()) {
            if (tab != menu.getTab()) renderTab(graphics, tab, false);
        }

        graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        int maxRow = menu.getMaxRowOffset();
        int knobY = y + SCROLL_Y;
        if (maxRow > 0) knobY += (SCROLL_HEIGHT - 17) * menu.getRowOffset() / maxRow;
        graphics.blit(TABS_TEXTURE, x + SCROLL_X, knobY, (maxRow > 0) ? 232 : 244, 0, 12, 15);

        renderTab(graphics, menu.getTab(), true);

        // Selected build backdrop
        if (menu.getSelected() != null) {
            for (int i = 0; i < AlmanacMenu.GRID_SIZE; ++i) {
                if (menu.getEntry(i) != menu.getSelected()) continue;
                Slot slot = menu.slots.get(i);
                graphics.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, SELECTED_COLOR);
            }
        }
    }

    private void renderTab(GuiGraphics graphics, AlmanacCategory tab, boolean selected) {
        int x = this.leftPos + tab.ordinal() * TAB_WIDTH;
        int y = this.topPos - 28;
        graphics.blit(TABS_TEXTURE, x, y, tab.ordinal() * 26, selected ? 32 : 0, 26, 32);
        graphics.renderItem(tab.icon, x + 5, y + 9);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, menu.getTab().getTitle(), 8, 6, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        for (AlmanacCategory tab : AlmanacCategory.values()) {
            if (isOverTab(tab, mouseX, mouseY))
                graphics.renderTooltip(this.font, tab.getTitle(), mouseX, mouseY);
        }
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (AlmanacCategory tab : AlmanacCategory.values()) {
                if (isOverTab(tab, mouseX, mouseY)) {
                    if (tab != menu.getTab()) sendButton(tab.ordinal());
                    return true;
                }
            }

            int scrollX = this.leftPos + SCROLL_X;
            int scrollY = this.topPos + SCROLL_Y;
            if (mouseX >= scrollX && mouseX < scrollX + 14 && mouseY >= scrollY && mouseY < scrollY + SCROLL_HEIGHT) {
                scrolling = true;
                scrollTo(mouseY);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) scrolling = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrolling) {
            scrollTo(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        setRowOffset(menu.getRowOffset() - (int) Math.signum(delta));
        return true;
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
        for (AlmanacCategory tab : AlmanacCategory.values()) {
            if (isOverTab(tab, mouseX, mouseY)) return false;
        }
        return super.hasClickedOutside(mouseX, mouseY, left, top, button);
    }

    private void scrollTo(double mouseY) {
        float pos = ((float) mouseY - this.topPos - SCROLL_Y - 7.5F) / (SCROLL_HEIGHT - 15);
        setRowOffset(Math.round(pos * menu.getMaxRowOffset()));
    }

    private void setRowOffset(int row) {
        row = Mth.clamp(row, 0, menu.getMaxRowOffset());
        if (row != menu.getRowOffset()) sendButton(AlmanacMenu.SCROLL_BUTTON_OFFSET + row);
    }

    // Apply locally for an instant update, then mirror on the server
    private void sendButton(int id) {
        menu.clickMenuButton(this.minecraft.player, id);
        this.minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    private boolean isOverTab(AlmanacCategory tab, double mouseX, double mouseY) {
        int x = this.leftPos + tab.ordinal() * TAB_WIDTH;
        return mouseX >= x && mouseX < x + TAB_WIDTH && mouseY >= this.topPos - 32 && mouseY < this.topPos;
    }
}
