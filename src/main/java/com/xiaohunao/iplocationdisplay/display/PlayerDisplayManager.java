package com.xiaohunao.iplocationdisplay.display;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.xiaohunao.iplocationdisplay.config.IpLocationConfig;
import com.xiaohunao.iplocationdisplay.location.CachedLocationResolver;
import com.xiaohunao.iplocationdisplay.location.IpLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
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
    private static final int DUPLICATE_CLEANUP_INTERVAL_TICKS = 20;

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

        cleanupTaggedDisplays(server, playerId);
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

    public void onPlayerStartTracking(ServerPlayer player, Entity target) {
        if (DisplayVisibility.isOwnedTextDisplay(player.getUUID(), target)) {
            hideDisplayFromOwner(player, target);
        }
    }

    public void tick(MinecraftServer server) {
        if (!settings.enabled() || displays.isEmpty()) {
            return;
        }

        ticks++;
        if (ticks % settings.tickInterval() != 0) {
            return;
        }

        boolean cleanupDuplicates = ticks % DUPLICATE_CLEANUP_INTERVAL_TICKS == 0;
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
                cleanupTaggedDisplays(server, entry.getKey());
                spawn(player, state.text());
                continue;
            }

            if (cleanupDuplicates) {
                cleanupTaggedDisplays(server, entry.getKey(), display.getId());
            }
            if (state.attached()) {
                keepAttached(player, display);
            } else {
                moveDisplay(player, display);
            }
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
        cleanupTaggedDisplays(server, player.getUUID());

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

        display.addTag(DisplayVisibility.ownerTag(player.getUUID()));
        display.load(textDisplayNbt(text, displayRenderYOffset(player)));
        display.setNoGravity(true);
        display.setInvulnerable(true);
        positionDisplay(player, display);

        if (!level.addFreshEntity(display)) {
            display.discard();
            LOGGER.warn("Failed to add IP location display entity for {}", player.getGameProfile().getName());
            return;
        }

        boolean attached = attachDisplay(player, display);
        displays.put(player.getUUID(), new DisplayState(display.getId(), text, level.dimension(), attached));
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

    private void cleanupTaggedDisplays(MinecraftServer server, UUID playerId) {
        cleanupTaggedDisplays(server, playerId, null);
    }

    private void cleanupTaggedDisplays(MinecraftServer server, UUID playerId, Integer keptEntityId) {
        String tag = DisplayVisibility.ownerTag(playerId);
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity.getType() == EntityType.TEXT_DISPLAY
                        && entity.getTags().contains(tag)
                        && (keptEntityId == null || entity.getId() != keptEntityId)) {
                    entity.discard();
                }
            }
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
        positionDisplay(player, display);
        player.serverLevel().getChunkSource().broadcast(display, new ClientboundTeleportEntityPacket(display));
    }

    private void positionDisplay(ServerPlayer player, Entity display) {
        display.setPos(player.getX(), player.getY() + settings.verticalOffset(), player.getZ());
    }

    private boolean attachDisplay(ServerPlayer player, Entity display) {
        if (!display.startRiding(player, true)) {
            LOGGER.warn("Failed to attach IP location display entity to {}", player.getGameProfile().getName());
            return false;
        }

        player.positionRider(display);
        player.serverLevel().getChunkSource().broadcast(player, new ClientboundSetPassengersPacket(player));
        hideDisplayFromOwner(player, display);
        return true;
    }

    private void keepAttached(ServerPlayer player, Entity display) {
        if (display.getVehicle() != player && !attachDisplay(player, display)) {
            moveDisplay(player, display);
            return;
        }

        player.positionRider(display);
        hideDisplayFromOwner(player, display);
    }

    private void hideDisplayFromOwner(ServerPlayer player, Entity display) {
        player.connection.send(new ClientboundRemoveEntitiesPacket(display.getId()));
    }

    private double displayRenderYOffset(ServerPlayer player) {
        return Math.max(0.0D, settings.verticalOffset() - player.getBbHeight());
    }

    private String remoteAddress(ServerPlayer player) {
        String address = remoteAddressReader.read(player.connection);
        if (address.isBlank()) {
            LOGGER.debug("Unable to read remote address for {}", player.getGameProfile().getName());
        }
        return address;
    }

    private CompoundTag textDisplayNbt(String text, double renderYOffset) {
        JsonObject json = new JsonObject();
        json.addProperty("text", text);
        json.addProperty("color", "gold");

        CompoundTag tag = new CompoundTag();
        tag.putString("text", json.toString());
        tag.putString("billboard", "center");
        tag.putInt("background", 1073741824);
        tag.putBoolean("shadow", true);
        tag.putBoolean("see_through", false);
        tag.putInt("teleport_duration", 0);
        tag.putInt("interpolation_duration", 0);
        tag.put("transformation", transformationNbt(renderYOffset));
        return tag;
    }

    private CompoundTag transformationNbt(double renderYOffset) {
        CompoundTag transformation = new CompoundTag();
        transformation.put("translation", floatList(0.0F, (float) renderYOffset, 0.0F));
        transformation.put("scale", floatList(1.0F, 1.0F, 1.0F));
        transformation.put("left_rotation", floatList(0.0F, 0.0F, 0.0F, 1.0F));
        transformation.put("right_rotation", floatList(0.0F, 0.0F, 0.0F, 1.0F));
        return transformation;
    }

    private ListTag floatList(float... values) {
        ListTag list = new ListTag();
        for (float value : values) {
            list.add(FloatTag.valueOf(value));
        }
        return list;
    }

    private record DisplayState(int entityId, String text, ResourceKey<Level> dimension, boolean attached) {
    }
}
