package com.xiaohunao.iplocationdisplay;

import com.mojang.logging.LogUtils;
import com.xiaohunao.iplocationdisplay.config.IpLocationConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

@Mod(IpLocationDisplayMod.MOD_ID)
public final class IpLocationDisplayMod {
    public static final String MOD_ID = "iplocationdisplay";
    private static final Logger LOGGER = LogUtils.getLogger();

    public IpLocationDisplayMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, IpLocationConfig.SPEC);
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
    }

    private void onServerStarting(ServerStartingEvent event) {
        if (IpLocationConfig.ENABLED.get()) {
            LOGGER.info("IP Location Display enabled");
        }
    }

    private void onServerStopping(ServerStoppingEvent event) {
        LOGGER.debug("IP Location Display stopping");
    }
}
