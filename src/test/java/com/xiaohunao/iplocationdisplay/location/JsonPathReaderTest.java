package com.xiaohunao.iplocationdisplay.location;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonPathReaderTest {
    private final JsonPathReader reader = new JsonPathReader();

    @Test
    void readsTopLevelString() {
        JsonObject root = JsonParser.parseString("{\"status\":\"success\"}").getAsJsonObject();

        assertEquals(Optional.of("success"), reader.read(root, "status"));
    }

    @Test
    void readsDottedNestedString() {
        JsonObject root = JsonParser.parseString("{\"data\":{\"region\":\"广东\"}}").getAsJsonObject();

        assertEquals(Optional.of("广东"), reader.read(root, "data.region"));
    }

    @Test
    void returnsEmptyForMissingPath() {
        JsonObject root = JsonParser.parseString("{\"data\":{}}").getAsJsonObject();

        assertEquals(Optional.empty(), reader.read(root, "data.region"));
    }
}
