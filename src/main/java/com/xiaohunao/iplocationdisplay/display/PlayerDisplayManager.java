package com.xiaohunao.iplocationdisplay.display;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.xiaohunao.iplocationdisplay.config.IpLocationConfig;
import com.xiaohunao.iplocationdisplay.location.CachedLocationResolver;
import com.xiaohunao.iplocationdisplay.location.IpLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerDisplayManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final CachedLocationResolver resolver;
    private final DisplayTextFormatter displayTextFormatter;
    private final RemoteAddressReader remoteAddressReader;
    private final PlaytimeReader playtimeReader;
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
        this.playtimeReader = new PlaytimeReader(settings.playtimeHourThreshold());
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

        discard(server, previous);
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
                discard(server, entry.getValue());
                iterator.remove();
                continue;
            }

            DisplayState state = entry.getValue();
            if (!state.dimension().equals(player.level().dimension())) {
                discard(server, state);
                spawn(player, state.text());
                continue;
            }

            Entity display = displayEntity(server, state);
            if (display == null) {
                spawn(player, state.text());
                continue;
            }

            moveDisplay(player, display);
        }
    }

    public void shutdown(MinecraftServer server) {
        displays.values().forEach(state -> discard(server, state));
        displays.clear();
    }

    private void createOrReplace(ServerPlayer player, IpLocation ipLocation) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        DisplayState previous = displays.remove(player.getUUID());
        if (previous != null) {
            discard(server, previous);
        }

        String playtimeStr = settings.showPlaytime()
            ? playtimeReader.getPlaytime(player)
            : "";

        String text = displayTextFormatter.format(settings.displayFormat(), ipLocation.value(), playtimeStr);
        if (!text.isBlank()) {
            spawn(player, text);
        }
    }

    private void spawn(ServerPlayer player, String text) {
        ServerLevel level = player.serverLevel();
        Display.TextDisplay display = EntityType.TEXT_DISPLAY.create(level);
        if (display == null) {
            LOGGER.warn("Failed to create IP location display entity for {}", player.getGameProfile().getName());
            return;
        }

        display.addTag(tag(player.getUUID()));
        display.load(textDisplayNbt(text));
        display.setNoGravity(true);
        display.setInvulnerable(true);
        moveDisplay(player, display);

        if (!level.addFreshEntity(display)) {
            display.discard();
            LOGGER.warn("Failed to add IP location display entity for {}", player.getGameProfile().getName());
            return;
        }

        displays.put(player.getUUID(), new DisplayState(display.getId(), text, level.dimension()));
    }

    private void remove(MinecraftServer server, UUID playerId) {
        DisplayState state = displays.remove(playerId);
        if (state != null) {
            discard(server, state);
        }
    }

    private void discard(MinecraftServer server, DisplayState state) {
        Entity display = displayEntity(server, state);
        if (display != null) {
            display.discard();
        }
    }

    private Entity displayEntity(MinecraftServer server, DisplayState state) {
        ServerLevel level = server.getLevel(state.dimension());
        if (level == null) {
            return null;
        }
        return level.getEntity(state.entityId());
    }

    private void moveDisplay(ServerPlayer player, Entity display) {
        display.setPos(player.getX(), player.getY() + settings.verticalOffset(), player.getZ());
    }

    private String remoteAddress(ServerPlayer player) {
        String address = remoteAddressReader.read(player.connection);
        if (address.isBlank()) {
            LOGGER.debug("Unable to read remote address for {}", player.getGameProfile().getName());
        }
        return address;
    }

    private CompoundTag textDisplayNbt(String text) {
        JsonObject json = new JsonObject();
        json.addProperty("text", text);
        json.addProperty("color", "gold");

        CompoundTag tag = new CompoundTag();
        tag.putString("text", json.toString());
        tag.putString("billboard", "center");
        tag.putInt("background", 1073741824);
        tag.putBoolean("shadow", true);
        tag.putBoolean("see_through", false);
        tag.putInt("teleport_duration", Math.min(settings.tickInterval(), 59));
        return tag;
    }

    private String tag(UUID playerId) {
        return "ipld_" + playerId.toString().replace("-", "");
    }

    private record DisplayState(int entityId, String text, ResourceKey<Level> dimension) {
    }
}
