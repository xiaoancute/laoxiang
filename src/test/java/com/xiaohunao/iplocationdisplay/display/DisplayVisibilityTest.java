package com.xiaohunao.iplocationdisplay.display;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisplayVisibilityTest {
    @Test
    void matchesDisplayTagForOwnerOnly() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID other = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Set<String> tags = Set.of(DisplayVisibility.ownerTag(owner));

        assertTrue(DisplayVisibility.hasOwnerTag(owner, tags));
        assertFalse(DisplayVisibility.hasOwnerTag(other, tags));
    }

    @Test
    void doesNotMatchMissingEntity() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000001");

        assertFalse(DisplayVisibility.isOwnedTextDisplay(owner, null));
    }
}
