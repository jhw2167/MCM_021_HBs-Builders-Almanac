package com.holybuckets.builder.client;

import com.holybuckets.builder.client.screen.AlmanacScreen;
import com.holybuckets.builder.client.state.AlmanacBookState;
import com.holybuckets.builder.client.state.BookEvent;
import com.holybuckets.builder.client.state.BookState;
import com.holybuckets.builder.item.BuildersAlmanacItem;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.TickPhase;
import net.blay09.mods.balm.api.event.TickType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.core.animation.RawAnimation;

public class ClientAlmanacAnimator {

    private static final float HOVER_SPEED = 0.25F;
    private static final int PAGE_TURN_CHANCE = 160;   // one in 160 ticks, about every 8 seconds
    private static final int PAGE_TURN_TICKS = 13;     // length of page_flip
    private static final int SETTLE_TICKS = 30;        // lets the opening animation finish first
    private static final RandomSource RANDOM = RandomSource.create();

    private static BookState state = AlmanacBookState.CLOSED;
    private static int ticksInState = 0;
    private static boolean wasHeld = false;
    private static boolean wasMenu = false;
    private static boolean wasBuildMode = false;
    private static float hover = 0;
    private static float hoverO = 0;
    private static float buildBlend = 0;
    private static float buildBlendO = 0;

    public static void init() {
        Balm.getEvents().onTickEvent(TickType.Client, TickPhase.End, ClientAlmanacAnimator::onClientTick);
    }

    private static void onClientTick(Minecraft mc) {
        boolean held = mc.player != null && BuildersAlmanacItem.isHeld(mc.player);
        boolean menu = mc.screen instanceof AlmanacScreen;
        boolean buildMode = ClientBuilderManager.isBuildMode();

        if (held != wasHeld) fire(held ? BookEvent.HOVER : BookEvent.UNHOVER);
        if (menu != wasMenu) fire(menu ? BookEvent.MENU_OPEN : BookEvent.MENU_CLOSE);
        if (buildMode && !wasBuildMode) fire(BookEvent.BUILD_MODE);
        wasHeld = held;
        wasMenu = menu;
        wasBuildMode = buildMode;

        ticksInState++;
        if (state == AlmanacBookState.PAGE_TURN && ticksInState >= PAGE_TURN_TICKS)
            fire(BookEvent.PAGE_TURNED);
        else if (ticksInState >= SETTLE_TICKS && RANDOM.nextInt(PAGE_TURN_CHANCE) == 0)
            fire(BookEvent.PAGE_TURN);

        hoverO = hover;
        hover += ((state.isOpen() ? 1 : 0) - hover) * HOVER_SPEED;
        buildBlendO = buildBlend;
        buildBlend += ((buildMode ? 1 : 0) - buildBlend) * HOVER_SPEED;
    }

    public static void fire(BookEvent event) {
        BookState next = state.update(event);
        if (next == state) return;
        state.exit();
        state = next;
        ticksInState = 0;
        state.enter();
    }

    public static float getHover(float partialTicks) {
        return Mth.lerp(partialTicks, hoverO, hover);
    }

    /** Eases from 0 to 1 while build mode is active */
    public static float getBuildBlend(float partialTicks) {
        return Mth.lerp(partialTicks, buildBlendO, buildBlend);
    }

    // NONE is the context the two-handed render in MixinItemInHandRenderer draws with
    public static RawAnimation getAnimation(ItemDisplayContext context) {
        boolean hand = context != null && (context == ItemDisplayContext.NONE || context.firstPerson()
            || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
        return hand ? state.getAnimation() : AlmanacBookState.CLOSED.getAnimation();
    }

}
