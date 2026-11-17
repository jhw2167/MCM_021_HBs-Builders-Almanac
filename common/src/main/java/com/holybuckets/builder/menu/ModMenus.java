package com.holybuckets.builder.menu;

import com.holybuckets.builder.almanac.AlmanacCategory;
import com.holybuckets.builder.Constants;
import net.blay09.mods.balm.api.DeferredObject;
import net.blay09.mods.balm.api.menu.BalmMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ModMenus {

    public static DeferredObject<MenuType<AlmanacMenu>> almanacMenu;


    public static void initialize(BalmMenus menus)
    {
        almanacMenu = menus.registerMenu(id("almanac_menu"),
            (syncId, inventory, buf) -> new AlmanacMenu(syncId, inventory, buf.readEnum(InteractionHand.class), buf.readEnum(AlmanacCategory.class)));
    }

    private static ResourceLocation id(String name) {
        return new ResourceLocation(Constants.MOD_ID, name);
    }

}


