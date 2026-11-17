package com.holybuckets.builder.client.state;

import software.bernie.geckolib.core.animation.RawAnimation;

import java.util.EnumMap;
import java.util.Map;

public class AlmanacBookState implements BookState {

    public static final AlmanacBookState CLOSED = new AlmanacBookState(RawAnimation.begin().thenLoop("idle_closed"), false);
    public static final AlmanacBookState OPENING = new AlmanacBookState(RawAnimation.begin().thenPlay("opening").thenLoop("idle_open"), true);
    public static final AlmanacBookState IDLE = new AlmanacBookState(RawAnimation.begin().thenLoop("idle_open"), true);
    public static final AlmanacBookState FLIP = new AlmanacBookState(RawAnimation.begin().thenPlay("page_flip").thenLoop("idle_open"), true);
    public static final AlmanacBookState PAGE_TURN = new AlmanacBookState(RawAnimation.begin().thenPlay("page_flip").thenLoop("idle_open"), true);
    public static final AlmanacBookState CLOSING = new AlmanacBookState(RawAnimation.begin().thenPlay("closing").thenLoop("idle_closed"), false);

    static {
        CLOSED.on(BookEvent.HOVER, OPENING);
        OPENING.on(BookEvent.UNHOVER, CLOSING).on(BookEvent.MENU_OPEN, FLIP).on(BookEvent.BUILD_MODE, IDLE).on(BookEvent.PAGE_TURN, PAGE_TURN);
        IDLE.on(BookEvent.UNHOVER, CLOSING).on(BookEvent.MENU_OPEN, FLIP).on(BookEvent.PAGE_TURN, PAGE_TURN);
        PAGE_TURN.on(BookEvent.UNHOVER, CLOSING).on(BookEvent.MENU_OPEN, FLIP).on(BookEvent.PAGE_TURNED, IDLE);
        FLIP.on(BookEvent.UNHOVER, CLOSING).on(BookEvent.MENU_CLOSE, IDLE);
        CLOSING.on(BookEvent.HOVER, OPENING);
    }

    private final RawAnimation animation;
    private final boolean open;
    private final Map<BookEvent, BookState> transitions = new EnumMap<>(BookEvent.class);

    private AlmanacBookState(RawAnimation animation, boolean open) {
        this.animation = animation;
        this.open = open;
    }

    private AlmanacBookState on(BookEvent event, BookState next) {
        transitions.put(event, next);
        return this;
    }

    @Override
    public RawAnimation getAnimation() {
        return animation;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public BookState update(BookEvent event) {
        return transitions.getOrDefault(event, this);
    }

}
