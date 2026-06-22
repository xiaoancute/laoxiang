package com.xiaohunao.iplocationdisplay.location;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Optional;

public final class JsonPathReader {
    public Optional<String> read(JsonObject root, String path) {
        if (root == null || path == null || path.isBlank()) {
            return Optional.empty();
        }

        JsonElement current = root;
        for (String part : path.split("\\.")) {
            if (part.isBlank() || !current.isJsonObject()) {
                return Optional.empty();
            }
            current = current.getAsJsonObject().get(part);
            if (current == null || current.isJsonNull()) {
                return Optional.empty();
            }
        }

        if (!current.isJsonPrimitive()) {
            return Optional.empty();
        }

        String value = current.getAsString().trim();
        return value.isEmpty() ? Optional.empty() : Optional.of(value);
    }
}
