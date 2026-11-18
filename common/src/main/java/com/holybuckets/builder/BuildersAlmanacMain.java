package com.holybuckets.builder;


import com.holybuckets.builder.client.render.BuildIconMesher;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.builder.config.TemplateConfig;
import com.holybuckets.builder.core.AlmanacTemplates;
import com.holybuckets.builder.core.BuildManager;
import com.holybuckets.builder.core.MightyDesignConverter;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.EventPriority;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;

/**
 * Main instance of the mod, initialize this class statically via commonClass
 * This class will init all major Manager instances and events for the mod
 */
public class BuildersAlmanacMain {
    private static boolean DEV_MODE = false;;
    private static TemplateConfig CONFIG;
    public static BuildersAlmanacMain INSTANCE;

    public BuildersAlmanacMain()
    {
        super();
        INSTANCE = this;
        init();
        // LoggerProject.logInit( "001000", this.getClass().getName() ); // Uncomment if you have a logging system in place
    }

    private void init()
    {
        //Events
        EventRegistrar registrar = EventRegistrar.getInstance();
        BuildManager.init(registrar);
        MightyDesignConverter.init(registrar);
        AlmanacTemplates.init(registrar);
        BuildIconMesher.init(registrar);

        registrar.registerOnBeforeServerStarted(this::onServerStarting);

    }

    private void onServerStarting(ServerStartingEvent e) {
        this.DEV_MODE = false;
    }


}
