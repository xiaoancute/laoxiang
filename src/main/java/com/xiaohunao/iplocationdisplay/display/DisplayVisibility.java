package com.xiaohunao.iplocationdisplay.display;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.Set;
import java.util.UUID;

final class DisplayVisibility {
    private DisplayVisibility() {
    }

    static String ownerTag(UUID playerId) {
        return "ipld_" + playerId.toString().replace("-", "");
    }

    static boolean hasOwnerTag(UUID playerId, Set<String> tags) {
        return tags.contains(ownerTag(playerId));
    }

    static boolean isOwnedTextDisplay(UUID playerId, Entity entity) {
        return entity != null && entity.getType() == EntityType.TEXT_DISPLAY && hasOwnerTag(playerId, entity.getTags());
    }
}
