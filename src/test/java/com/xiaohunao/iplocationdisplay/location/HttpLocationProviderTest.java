package com.xiaohunao.iplocationdisplay.location;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpLocationProviderTest {
    @Test
    void resolvesSuccessfulIpApiResponse() throws Exception {
        FakeHttpLookupClient client = new FakeHttpLookupClient(new HttpLookupResponse(200, """
                {
                  "status": "success",
                  "country": "美国",
                  "regionName": "弗吉尼亚州",
                  "city": "Ashburn",
                  "query": "8.8.8.8"
                }
                """));
        HttpLocationProvider provider = provider(client);

        Optional<IpLocation> location = provider.lookup("8.8.8.8");

        assertEquals(Optional.of(new IpLocation("美国 弗吉尼亚州 Ashburn")), location);
        assertEquals("http://ip-api.com/json/8.8.8.8?lang=zh-CN", client.requestedUrl);
    }

    @Test
    void returnsEmptyWhenProviderStatusIsFail() throws Exception {
        FakeHttpLookupClient client = new FakeHttpLookupClient(new HttpLookupResponse(200, """
                {"status":"fail","message":"reserved range"}
                """));

        assertEquals(Optional.empty(), provider(client).lookup("192.0.2.1"));
    }

    @Test
    void resolvesIpSbResponseWithoutSuccessMarker() throws Exception {
        FakeHttpLookupClient client = new FakeHttpLookupClient(new HttpLookupResponse(200, """
                {
                  "country": "China",
                  "country_code": "CN",
                  "region": "Jiangxi",
                  "region_code": "JX",
                  "city": "Ganzhou",
                  "isp": "China Mobile communications corporation",
                  "ip": "39.176.146.30"
                }
                """));
        JsonPathReader jsonPathReader = new JsonPathReader();
        HttpLocationProvider provider = new HttpLocationProvider(
                "https://api.ip.sb/geoip/%ip%",
                "",
                "",
                "%country_localized% %region_localized% %city_localized% %isp_localized%",
                Duration.ofMillis(2000),
                client,
                jsonPathReader,
                new LocationTemplateFormatter(jsonPathReader)
        );

        Optional<IpLocation> location = provider.lookup("39.176.146.30");

        assertEquals(Optional.of(new IpLocation("中国 江西省 赣州市 移动")), location);
        assertEquals("https://api.ip.sb/geoip/39.176.146.30", client.requestedUrl);
    }

    @Test
    void resolvesForeignIpSbResponseWithRawIsp() throws Exception {
        FakeHttpLookupClient client = new FakeHttpLookupClient(new HttpLookupResponse(200, """
                {
                  "country": "United States",
                  "country_code": "US",
                  "region": "California",
                  "region_code": "CA",
                  "city": "Mountain View",
                  "isp": "Google LLC",
                  "ip": "8.8.8.8"
                }
                """));
        JsonPathReader jsonPathReader = new JsonPathReader();
        HttpLocationProvider provider = new HttpLocationProvider(
                "https://api.ip.sb/geoip/%ip%",
                "",
                "",
                "%country_localized% %region_localized% %city_localized% %isp_localized%",
                Duration.ofMillis(2000),
                client,
                jsonPathReader,
                new LocationTemplateFormatter(jsonPathReader)
        );

        Optional<IpLocation> location = provider.lookup("8.8.8.8");

        assertEquals(Optional.of(new IpLocation("United States California Mountain View Google LLC")), location);
        assertEquals("https://api.ip.sb/geoip/8.8.8.8", client.requestedUrl);
    }

    @Test
    void returnsEmptyForHttp429() throws Exception {
        FakeHttpLookupClient client = new FakeHttpLookupClient(new HttpLookupResponse(429, "Too Many Requests"));

        assertEquals(Optional.empty(), provider(client).lookup("8.8.8.8"));
    }

    @Test
    void returnsEmptyForInvalidJson() throws Exception {
        FakeHttpLookupClient client = new FakeHttpLookupClient(new HttpLookupResponse(200, "not json"));

        assertEquals(Optional.empty(), provider(client).lookup("8.8.8.8"));
    }

    @Test
    void returnsEmptyForEmptyFormattedLocation() throws Exception {
        FakeHttpLookupClient client = new FakeHttpLookupClient(new HttpLookupResponse(200, """
                {"status":"success"}
                """));

        assertEquals(Optional.empty(), provider(client).lookup("8.8.8.8"));
    }

    @Test
    void returnsEmptyWhenClientThrows() throws Exception {
        FakeHttpLookupClient client = new FakeHttpLookupClient(new RuntimeException("timeout"));

        assertEquals(Optional.empty(), provider(client).lookup("8.8.8.8"));
    }

    private HttpLocationProvider provider(HttpLookupClient client) {
        JsonPathReader jsonPathReader = new JsonPathReader();
        return new HttpLocationProvider(
                "http://ip-api.com/json/%ip%?lang=zh-CN",
                "status",
                "success",
                "%country% %regionName% %city%",
                Duration.ofMillis(2000),
                client,
                jsonPathReader,
                new LocationTemplateFormatter(jsonPathReader)
        );
    }

    private static final class FakeHttpLookupClient implements HttpLookupClient {
        private final HttpLookupResponse response;
        private final RuntimeException exception;
        private String requestedUrl;

        private FakeHttpLookupClient(HttpLookupResponse response) {
            this.response = response;
            this.exception = null;
        }

        private FakeHttpLookupClient(RuntimeException exception) {
            this.response = null;
            this.exception = exception;
        }

        @Override
        public HttpLookupResponse get(String url, Duration timeout) {
            this.requestedUrl = url;
            if (exception != null) {
                throw exception;
            }
            return response;
        }
    }
}
