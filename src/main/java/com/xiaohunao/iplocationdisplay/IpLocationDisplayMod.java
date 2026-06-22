package com.xiaohunao.iplocationdisplay;

import com.mojang.logging.LogUtils;
import com.xiaohunao.iplocationdisplay.config.IpLocationConfig;
import com.xiaohunao.iplocationdisplay.display.DisplayTextFormatter;
import com.xiaohunao.iplocationdisplay.display.PlayerDisplayManager;
import com.xiaohunao.iplocationdisplay.location.AddressNormalizer;
import com.xiaohunao.iplocationdisplay.location.CachedLocationResolver;
import com.xiaohunao.iplocationdisplay.location.HttpLocationProvider;
import com.xiaohunao.iplocationdisplay.location.HttpProviderPreset;
import com.xiaohunao.iplocationdisplay.location.Ip2RegionLocationProvider;
import com.xiaohunao.iplocationdisplay.location.JdkHttpLookupClient;
import com.xiaohunao.iplocationdisplay.location.JsonPathReader;
import com.xiaohunao.iplocationdisplay.location.LocationProvider;
import com.xiaohunao.iplocationdisplay.location.LocationProviders;
import com.xiaohunao.iplocationdisplay.location.LocationTemplateFormatter;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mod(IpLocationDisplayMod.MOD_ID)
public final class IpLocationDisplayMod {
    public static final String MOD_ID = "iplocationdisplay";
    private static final Logger LOGGER = LogUtils.getLogger();

    private ExecutorService lookupExecutor;
    private CachedLocationResolver resolver;
    private PlayerDisplayManager displayManager;

    public IpLocationDisplayMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("IP Location Display mod loaded");
        modContainer.registerConfig(
                ModConfig.Type.valueOf(IpLocationConfig.CONFIG_TYPE_NAME),
                IpLocationConfig.SPEC,
                IpLocationConfig.CONFIG_FILE_NAME
        );
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(this::onPlayerChangedDimension);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
    }

    private void onServerStarting(ServerStartingEvent event) {
        IpLocationConfig.RuntimeSettings settings = IpLocationConfig.runtimeSettings();
        LOGGER.info("IP Location Display config: enabled={}, providerMode={}, displayFormat={}, showUnknown={}",
                settings.enabled(), settings.providerMode(), settings.displayFormat(), settings.showUnknown());
        if (settings.enabled()) {
            JsonPathReader jsonPathReader = new JsonPathReader();
            LocationProvider localProvider = new Ip2RegionLocationProvider(settings.databasePath());
            HttpProviderPreset httpPreset = HttpProviderPreset.resolve(
                    settings.httpPreset(),
                    settings.httpUrlTemplate(),
                    settings.httpSuccessJsonPath(),
                    settings.httpSuccessValue(),
                    settings.httpLocationTemplate()
            );
            LocationProvider httpProvider = new HttpLocationProvider(
                    httpPreset.urlTemplate(),
                    httpPreset.successJsonPath(),
                    httpPreset.successValue(),
                    httpPreset.locationTemplate(),
                    Duration.ofMillis(settings.httpTimeoutMillis()),
                    new JdkHttpLookupClient(),
                    jsonPathReader,
                    new LocationTemplateFormatter(jsonPathReader)
            );
            LocationProvider provider = LocationProviders.choose(settings.providerMode(), localProvider, httpProvider);
            lookupExecutor = Executors.newFixedThreadPool(2, runnable -> {
                Thread thread = new Thread(runnable, "ip-location-display-lookup");
                thread.setDaemon(true);
                return thread;
            });
            resolver = new CachedLocationResolver(
                    new AddressNormalizer(),
                    provider,
                    lookupExecutor,
                    settings.localText(),
                    settings.unknownText(),
                    settings.showUnknown()
            );
            displayManager = new PlayerDisplayManager(resolver, new DisplayTextFormatter(), settings);
            LOGGER.info("IP Location Display enabled");
        }
    }

    private void onServerStopping(ServerStoppingEvent event) {
        if (displayManager != null) {
            displayManager.shutdown(event.getServer());
            displayManager = null;
        }
        if (resolver != null) {
            resolver.shutdown();
            resolver = null;
        }
        if (lookupExecutor != null) {
            lookupExecutor.shutdownNow();
            lookupExecutor = null;
        }
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (displayManager != null && event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            displayManager.onPlayerLogin(player);
        }
    }

    private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (displayManager != null && event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            displayManager.onPlayerLogout(player);
        }
    }

    private void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (displayManager != null && event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            displayManager.onPlayerChangedDimension(player);
        }
    }

    private void onServerTick(ServerTickEvent.Post event) {
        if (displayManager != null) {
            displayManager.tick(event.getServer());
        }
    }
}
