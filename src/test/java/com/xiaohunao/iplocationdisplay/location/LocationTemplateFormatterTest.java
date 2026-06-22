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

    @Test
    void localizesAdditionalChinaCityFieldsFromIpSb() {
        JsonObject root = JsonParser.parseString("""
                {
                  "country": "China",
                  "country_code": "CN",
                  "region": "Guangdong",
                  "region_code": "GD",
                  "city": "Shenzhen"
                }
                """).getAsJsonObject();

        assertEquals("中国 广东省 深圳市", formatter.format(root, "%country_localized% %region_localized% %city_localized%"));
    }

    @Test
    void localizesChinaCitiesWithCommonEnglishAliases() {
        JsonObject root = JsonParser.parseString("""
                {
                  "country": "China",
                  "country_code": "CN",
                  "region": "Heilongjiang",
                  "region_code": "HL",
                  "city": "Harbin"
                }
                """).getAsJsonObject();

        assertEquals("中国 黑龙江省 哈尔滨市", formatter.format(root, "%country_localized% %region_localized% %city_localized%"));
    }

    @Test
    void localizesChinaCitiesWithApostrophes() {
        JsonObject root = JsonParser.parseString("""
                {
                  "country": "China",
                  "country_code": "CN",
                  "region": "Shaanxi",
                  "region_code": "SN",
                  "city": "Xi'an"
                }
                """).getAsJsonObject();

        assertEquals("中国 陕西省 西安市", formatter.format(root, "%country_localized% %region_localized% %city_localized%"));
    }

    @Test
    void localizesChineseIspFromIpSbFields() {
        JsonObject root = JsonParser.parseString("""
                {
                  "isp": "CHINANET Jiangxi province network",
                  "organization": "China Telecom",
                  "asn_organization": "China Telecom"
                }
                """).getAsJsonObject();

        assertEquals("电信", formatter.format(root, "%isp_localized%"));
    }

    @Test
    void localizesChineseIspFromIpApiFields() {
        JsonObject root = JsonParser.parseString("""
                {
                  "isp": "China Mobile communications corporation",
                  "org": "CMNET",
                  "as": "AS9808 China Mobile Communications Group Co., Ltd.",
                  "asname": "CMNET-GD"
                }
                """).getAsJsonObject();

        assertEquals("移动", formatter.format(root, "%isp_localized%"));
    }

    @Test
    void fallsBackToRawIspWhenNoChineseOperatorMatches() {
        JsonObject root = JsonParser.parseString("{\"isp\":\"Example Hosting\"}").getAsJsonObject();

        assertEquals("United States California Example Hosting", formatter.format(root,
                "United States California %isp_localized%"));
    }

    @Test
    void stripsAsnPrefixFromRawIspFallback() {
        JsonObject root = JsonParser.parseString("{\"as\":\"AS15169 Google LLC\"}").getAsJsonObject();

        assertEquals("Google LLC", formatter.format(root, "%isp_localized%"));
    }
}
