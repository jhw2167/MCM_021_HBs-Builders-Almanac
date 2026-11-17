package com.holybuckets.builder.item;

import com.holybuckets.builder.Constants;
import com.holybuckets.builder.client.ClientAlmanacAnimator;
import com.holybuckets.builder.core.BuildManager;
import com.holybuckets.builder.menu.AlmanacMenu;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.menu.BalmMenuProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public abstract class BuildersAlmanacItem extends Item implements GeoItem {

    private static final int TRANSITION_TICKS = 3;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public BuildersAlmanacItem() {
        super(Balm.getItems().itemProperties().stacksTo(1));
    }

    @Override
    public boolean isPerspectiveAware() {
        return true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "book", TRANSITION_TICKS, BuildersAlmanacItem::animationStateController));
    }

        private static PlayState animationStateController(AnimationState<BuildersAlmanacItem> state) {
            RawAnimation animation = ClientAlmanacAnimator.getAnimation(state.getData(DataTickets.ITEM_RENDER_PERSPECTIVE));
            return state.setAndContinue(animation);
        }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            BuildManager.get(player).enterBuildMode((ServerPlayer) player);
            Balm.getNetworking().openMenu(player, getMenuProvider(hand));
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    public static boolean isHeld(Player player) {
        return player.getMainHandItem().getItem() instanceof BuildersAlmanacItem
            || player.getOffhandItem().getItem() instanceof BuildersAlmanacItem;
    }

    public BalmMenuProvider getMenuProvider(InteractionHand hand) {
        return new BalmMenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("container." + Constants.MOD_ID + ".almanac");
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
                return new AlmanacMenu(syncId, playerInventory, hand, BuildManager.get(player).getLastTab());
            }

            @Override
            public void writeScreenOpeningData(ServerPlayer player, FriendlyByteBuf buf) {
                buf.writeEnum(hand);
                buf.writeEnum(BuildManager.get(player).getLastTab());
            }
        };
    }
}
