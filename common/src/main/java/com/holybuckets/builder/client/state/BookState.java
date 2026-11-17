package com.holybuckets.builder.client.state;

import software.bernie.geckolib.core.animation.RawAnimation;

public interface BookState {

    RawAnimation getAnimation();

    boolean isOpen();

    BookState update(BookEvent event);

    default void enter() {}

    default void exit() {}

}
