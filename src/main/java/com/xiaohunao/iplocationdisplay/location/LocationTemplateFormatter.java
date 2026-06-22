package com.xiaohunao.iplocationdisplay.location;

import com.google.gson.JsonObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LocationTemplateFormatter {
    private static final Pattern TOKEN = Pattern.compile("%([A-Za-z0-9_.-]+)%");

    private final JsonPathReader jsonPathReader;

    public LocationTemplateFormatter(JsonPathReader jsonPathReader) {
        this.jsonPathReader = jsonPathReader;
    }

    public String format(JsonObject root, String template) {
        if (template == null || template.isBlank()) {
            return "";
        }

        Matcher matcher = TOKEN.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String path = matcher.group(1);
            String value = jsonPathReader.read(root, path).orElse("");
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);

        return result.toString().replaceAll("\\s+", " ").trim();
    }
}
