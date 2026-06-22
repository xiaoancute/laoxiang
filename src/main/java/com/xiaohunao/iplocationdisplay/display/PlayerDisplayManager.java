package com.xiaohunao.iplocationdisplay.display;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.xiaohunao.iplocationdisplay.config.IpLocationConfig;
import com.xiaohunao.iplocationdisplay.location.CachedLocationResolver;
import com.xiaohunao.iplocationdisplay.location.IpLocation;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerDisplayManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final CachedLocationResolver resolver;
    private final DisplayTextFormatter displayTextFormatter;
    private final RemoteAddressReader remoteAddressReader;
    private final IpLocationConfig.RuntimeSettings settings;
    private final Map<UUID, DisplayState> displays = new ConcurrentHashMap<>();
    private int ticks;

    public PlayerDisplayManager(
            CachedLocationResolver resolver,
            DisplayTextFormatter displayTextFormatter,
            IpLocationConfig.RuntimeSettings settings
    ) {
        this.resolver = resolver;
        this.displayTextFormatter = displayTextFormatter;
        this.remoteAddressReader = new RemoteAddressReader();
        this.settings = settings;
    }

    public void onPlayerLogin(ServerPlayer player) {
        if (!settings.enabled()) {
            return;
        }

        UUID playerId = player.getUUID();
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        String remoteAddress = remoteAddress(player);
        LOGGER.info("Resolving IP location for {} from {}", player.getGameProfile().getName(), remoteAddress.isBlank() ? "<empty>" : remoteAddress);
        resolver.resolve(remoteAddress).thenAccept(location -> {
            if (location.isEmpty()) {
                LOGGER.info("No IP location display for {} because lookup returned empty", player.getGameProfile().getName());
                return;
            }
            IpLocation ipLocation = location.get();
            LOGGER.info("Resolved IP location for {}: {}", player.getGameProfile().getName(), ipLocation.value());
            server.execute(() -> {
                    ServerPlayer current = server.getPlayerList().getPlayer(playerId);
                    if (current != null) {
                        createOrReplace(current, ipLocation);
                    }
                });
        });
    }

    public void onPlayerLogout(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server != null) {
            remove(server, player.getUUID());
        }
    }

    public void onPlayerChangedDimension(ServerPlayer player) {
        DisplayState previous = displays.remove(player.getUUID());
        MinecraftServer server = player.getServer();
        if (previous == null || server == null) {
            return;
        }

        kill(server, previous);
        spawn(player, previous.text());
    }

    public void tick(MinecraftServer server) {
        if (!settings.enabled() || displays.isEmpty()) {
            return;
        }

        ticks++;
        if (ticks % settings.tickInterval() != 0) {
            return;
        }

        Iterator<Map.Entry<UUID, DisplayState>> iterator = displays.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, DisplayState> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                kill(server, entry.getValue());
                iterator.remove();
                continue;
            }

            DisplayState state = entry.getValue();
            if (!state.dimension().equals(player.level().dimension())) {
                kill(server, state);
                spawn(player, state.text());
                continue;
            }

            teleport(player, state.tag());
        }
    }

    public void shutdown(MinecraftServer server) {
        displays.values().forEach(state -> kill(server, state));
        displays.clear();
    }

    private void createOrReplace(ServerPlayer player, IpLocation ipLocation) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        DisplayState previous = displays.remove(player.getUUID());
        if (previous != null) {
            kill(server, previous);
        }

        String text = displayTextFormatter.format(settings.displayFormat(), ipLocation.value());
        if (!text.isBlank()) {
            spawn(player, text);
        }
    }

    private void spawn(ServerPlayer player, String text) {
        String tag = tag(player.getUUID());
        ResourceKey<Level> dimension = player.level().dimension();
        displays.put(player.getUUID(), new DisplayState(tag, text, dimension));

        String command = String.format(Locale.ROOT,
                "execute in %s run summon minecraft:text_display %.3f %.3f %.3f %s",
                dimension.location(),
                player.getX(),
                player.getY() + settings.verticalOffset(),
                player.getZ(),
                textDisplayNbt(tag, text)
        );
        runCommand(player.getServer(), command);
    }

    private void teleport(ServerPlayer player, String tag) {
        String command = String.format(Locale.ROOT,
                "execute in %s run tp @e[type=minecraft:text_display,tag=%s,limit=1] %.3f %.3f %.3f",
                player.level().dimension().location(),
                tag,
                player.getX(),
                player.getY() + settings.verticalOffset(),
                player.getZ()
        );
        runCommand(player.getServer(), command);
    }

    private void remove(MinecraftServer server, UUID playerId) {
        DisplayState state = displays.remove(playerId);
        if (state != null) {
            kill(server, state);
        }
    }

    private void kill(MinecraftServer server, DisplayState state) {
        String command = String.format(Locale.ROOT,
                "execute in %s run kill @e[type=minecraft:text_display,tag=%s]",
                state.dimension().location(),
                state.tag()
        );
        runCommand(server, command);
    }

    private void runCommand(MinecraftServer server, String command) {
        if (server == null) {
            return;
        }
        try {
            CommandSourceStack source = server.createCommandSourceStack()
                    .withSuppressedOutput()
                    .withPermission(4);
            server.getCommands().performPrefixedCommand(source, command);
            LOGGER.debug("Ran IP location display command: {}", command);
        } catch (Exception exception) {
            LOGGER.warn("Failed to run IP location display command: {}", command, exception);
        }
    }

    private String remoteAddress(ServerPlayer player) {
        String address = remoteAddressReader.read(player.connection);
        if (address.isBlank()) {
            LOGGER.debug("Unable to read remote address for {}", player.getGameProfile().getName());
        }
        return address;
    }

    private String textDisplayNbt(String tag, String text) {
        JsonObject json = new JsonObject();
        json.addProperty("text", text);
        json.addProperty("color", "gold");

        return String.format(Locale.ROOT,
                "{Tags:[\"%s\"],text:'%s',billboard:\"center\",background:1073741824,shadow:1b,see_through:0b,NoGravity:1b,Invulnerable:1b}",
                tag,
                snbtSingleQuoted(json.toString())
        );
    }

    private String snbtSingleQuoted(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    private String tag(UUID playerId) {
        return "ipld_" + playerId.toString().replace("-", "");
    }

    private record DisplayState(String tag, String text, ResourceKey<Level> dimension) {
    }
}
