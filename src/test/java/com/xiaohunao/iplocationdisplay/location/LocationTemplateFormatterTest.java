package com.xiaohunao.iplocationdisplay.location;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocationTemplateFormatterTest {
    private final LocationTemplateFormatter formatter = new LocationTemplateFormatter(new JsonPathReader());

    @Test
    void formatsLocationFromJsonFields() {
        JsonObject root = JsonParser.parseString("""
                {
                  "country": "中国",
                  "regionName": "广东",
                  "city": "广州"
                }
                """).getAsJsonObject();

        assertEquals("中国 广东 广州", formatter.format(root, "%country% %regionName% %city%"));
    }

    @Test
    void omitsMissingValuesAndCollapsesWhitespace() {
        JsonObject root = JsonParser.parseString("{\"country\":\"中国\",\"city\":\"广州\"}").getAsJsonObject();

        assertEquals("中国 广州", formatter.format(root, "%country% %regionName% %city%"));
    }

    @Test
    void supportsDottedJsonPathsInsideTemplate() {
        JsonObject root = JsonParser.parseString("{\"data\":{\"region\":\"广东\"}}").getAsJsonObject();

        assertEquals("广东", formatter.format(root, "%data.region%"));
    }

    @Test
    void localizesIpSbChinaProvinceAndCityFields() {
        JsonObject root = JsonParser.parseString("""
                {
                  "country": "China",
                  "country_code": "CN",
                  "region": "Jiangxi",
                  "region_code": "JX",
                  "city": "Ganzhou"
                }
                """).getAsJsonObject();

        assertEquals("中国 江西省 赣州市", formatter.format(root, "%country_localized% %region_localized% %city_localized%"));
    }
}
