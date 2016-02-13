package pl.ludex.smartdashwallet.event;

import com.google.common.eventbus.EventBus;

/**
 * Created by Tomasz Ludek on 13/02/2016.
 */
public class MainEventBus {

    private static final EventBus eventBus = new EventBus();

    public static EventBus getDefault() {
        return eventBus;
    }
}
