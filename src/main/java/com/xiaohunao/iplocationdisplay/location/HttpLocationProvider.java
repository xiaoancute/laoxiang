package com.xiaohunao.iplocationdisplay.location;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

public final class HttpLocationProvider implements LocationProvider {
    private final String urlTemplate;
    private final String successJsonPath;
    private final String successValue;
    private final String locationTemplate;
    private final Duration timeout;
    private final HttpLookupClient httpLookupClient;
    private final JsonPathReader jsonPathReader;
    private final LocationTemplateFormatter locationTemplateFormatter;

    public HttpLocationProvider(
            String urlTemplate,
            String successJsonPath,
            String successValue,
            String locationTemplate,
            Duration timeout,
            HttpLookupClient httpLookupClient,
            JsonPathReader jsonPathReader,
            LocationTemplateFormatter locationTemplateFormatter
    ) {
        this.urlTemplate = urlTemplate;
        this.successJsonPath = successJsonPath;
        this.successValue = successValue;
        this.locationTemplate = locationTemplate;
        this.timeout = timeout;
        this.httpLookupClient = httpLookupClient;
        this.jsonPathReader = jsonPathReader;
        this.locationTemplateFormatter = locationTemplateFormatter;
    }

    @Override
    public Optional<IpLocation> lookup(String ip) {
        try {
            String url = urlTemplate.replace("%ip%", URLEncoder.encode(ip, StandardCharsets.UTF_8));
            HttpLookupResponse response = httpLookupClient.get(url, timeout);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Optional.empty();
            }

            JsonElement element = JsonParser.parseString(response.body());
            if (!element.isJsonObject()) {
                return Optional.empty();
            }

            boolean success = jsonPathReader.read(element.getAsJsonObject(), successJsonPath)
                    .map(value -> value.equalsIgnoreCase(successValue))
                    .orElse(false);
            if (!success) {
                return Optional.empty();
            }

            String location = locationTemplateFormatter.format(element.getAsJsonObject(), locationTemplate);
            return location.isEmpty() ? Optional.empty() : Optional.of(new IpLocation(location));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }
}
