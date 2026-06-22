package com.xiaohunao.iplocationdisplay.location;

import com.google.gson.JsonObject;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class LocationTemplateFormatter {
    private static final Pattern TOKEN = Pattern.compile("%([A-Za-z0-9_.-]+)%");
    private static final Map<String, String> CHINA_REGION_BY_CODE = Map.ofEntries(
            Map.entry("AH", "安徽省"),
            Map.entry("BJ", "北京市"),
            Map.entry("CQ", "重庆市"),
            Map.entry("FJ", "福建省"),
            Map.entry("GD", "广东省"),
            Map.entry("GS", "甘肃省"),
            Map.entry("GX", "广西壮族自治区"),
            Map.entry("GZ", "贵州省"),
            Map.entry("HA", "河南省"),
            Map.entry("HB", "湖北省"),
            Map.entry("HE", "河北省"),
            Map.entry("HI", "海南省"),
            Map.entry("HK", "香港特别行政区"),
            Map.entry("HL", "黑龙江省"),
            Map.entry("HN", "湖南省"),
            Map.entry("JL", "吉林省"),
            Map.entry("JS", "江苏省"),
            Map.entry("JX", "江西省"),
            Map.entry("LN", "辽宁省"),
            Map.entry("MO", "澳门特别行政区"),
            Map.entry("NM", "内蒙古自治区"),
            Map.entry("NX", "宁夏回族自治区"),
            Map.entry("QH", "青海省"),
            Map.entry("SC", "四川省"),
            Map.entry("SD", "山东省"),
            Map.entry("SH", "上海市"),
            Map.entry("SN", "陕西省"),
            Map.entry("SX", "山西省"),
            Map.entry("TJ", "天津市"),
            Map.entry("TW", "台湾省"),
            Map.entry("XJ", "新疆维吾尔自治区"),
            Map.entry("XZ", "西藏自治区"),
            Map.entry("YN", "云南省"),
            Map.entry("ZJ", "浙江省")
    );
    private static final Map<String, String> CHINA_REGION_BY_NAME = Map.ofEntries(
            Map.entry("anhui", "安徽省"),
            Map.entry("beijing", "北京市"),
            Map.entry("chongqing", "重庆市"),
            Map.entry("fujian", "福建省"),
            Map.entry("guangdong", "广东省"),
            Map.entry("gansu", "甘肃省"),
            Map.entry("guangxi", "广西壮族自治区"),
            Map.entry("guizhou", "贵州省"),
            Map.entry("henan", "河南省"),
            Map.entry("hubei", "湖北省"),
            Map.entry("hebei", "河北省"),
            Map.entry("hainan", "海南省"),
            Map.entry("hong kong", "香港特别行政区"),
            Map.entry("heilongjiang", "黑龙江省"),
            Map.entry("hunan", "湖南省"),
            Map.entry("jilin", "吉林省"),
            Map.entry("jiangsu", "江苏省"),
            Map.entry("jiangxi", "江西省"),
            Map.entry("liaoning", "辽宁省"),
            Map.entry("macao", "澳门特别行政区"),
            Map.entry("macau", "澳门特别行政区"),
            Map.entry("inner mongolia", "内蒙古自治区"),
            Map.entry("ningxia hui autonomous region", "宁夏回族自治区"),
            Map.entry("qinghai", "青海省"),
            Map.entry("sichuan", "四川省"),
            Map.entry("shandong", "山东省"),
            Map.entry("shanghai", "上海市"),
            Map.entry("shaanxi", "陕西省"),
            Map.entry("shanxi", "山西省"),
            Map.entry("tianjin", "天津市"),
            Map.entry("taiwan", "台湾省"),
            Map.entry("xinjiang", "新疆维吾尔自治区"),
            Map.entry("tibet", "西藏自治区"),
            Map.entry("yunnan", "云南省"),
            Map.entry("zhejiang", "浙江省")
    );
    private static final Map<String, String> CHINA_CITY_BY_REGION_AND_NAME = Map.ofEntries(
            Map.entry("JX:nanchang", "南昌市"),
            Map.entry("JX:jingdezhen", "景德镇市"),
            Map.entry("JX:pingxiang", "萍乡市"),
            Map.entry("JX:jiujiang", "九江市"),
            Map.entry("JX:xinyu", "新余市"),
            Map.entry("JX:yingtan", "鹰潭市"),
            Map.entry("JX:ganzhou", "赣州市"),
            Map.entry("JX:jian", "吉安市"),
            Map.entry("JX:ji'an", "吉安市"),
            Map.entry("JX:yichun", "宜春市"),
            Map.entry("JX:fuzhou", "抚州市"),
            Map.entry("JX:shangrao", "上饶市"),
            Map.entry("FJ:fuzhou", "福州市"),
            Map.entry("BJ:beijing", "北京市"),
            Map.entry("SH:shanghai", "上海市"),
            Map.entry("TJ:tianjin", "天津市"),
            Map.entry("CQ:chongqing", "重庆市")
    );

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
            String value = resolveToken(root, path).orElse("");
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);

        return result.toString().replaceAll("\\s+", " ").trim();
    }

    private Optional<String> resolveToken(JsonObject root, String path) {
        return switch (path) {
            case "country_localized" -> countryLocalized(root);
            case "region_localized" -> regionLocalized(root);
            case "city_localized" -> cityLocalized(root);
            case "isp_localized" -> ispLocalized(root);
            default -> jsonPathReader.read(root, path);
        };
    }

    private Optional<String> countryLocalized(JsonObject root) {
        Optional<String> countryCode = jsonPathReader.read(root, "country_code");
        Optional<String> country = jsonPathReader.read(root, "country");
        if (countryCode.map(code -> code.equalsIgnoreCase("CN")).orElse(false)
                || country.map(value -> value.equalsIgnoreCase("China")).orElse(false)) {
            return Optional.of("中国");
        }
        return country;
    }

    private Optional<String> regionLocalized(JsonObject root) {
        Optional<String> regionCode = jsonPathReader.read(root, "region_code");
        if (isChina(root) && regionCode.isPresent()) {
            String localized = CHINA_REGION_BY_CODE.get(regionCode.get().toUpperCase(Locale.ROOT));
            if (localized != null) {
                return Optional.of(localized);
            }
        }

        Optional<String> region = jsonPathReader.read(root, "region");
        if (isChina(root) && region.isPresent()) {
            String localized = CHINA_REGION_BY_NAME.get(region.get().toLowerCase(Locale.ROOT));
            if (localized != null) {
                return Optional.of(localized);
            }
        }
        return region;
    }

    private Optional<String> cityLocalized(JsonObject root) {
        Optional<String> city = jsonPathReader.read(root, "city");
        if (isChina(root) && city.isPresent()) {
            Optional<String> regionCode = jsonPathReader.read(root, "region_code");
            if (regionCode.isPresent()) {
                String localized = CHINA_CITY_BY_REGION_AND_NAME.get(
                        regionCode.get().toUpperCase(Locale.ROOT) + ":" + city.get().toLowerCase(Locale.ROOT)
                );
                if (localized != null) {
                    return Optional.of(localized);
                }
            }
        }
        return city;
    }

    private boolean isChina(JsonObject root) {
        return countryLocalized(root).map(value -> value.equals("中国")).orElse(false);
    }

    private Optional<String> ispLocalized(JsonObject root) {
        String value = ispParts()
                .map(path -> jsonPathReader.read(root, path))
                .flatMap(Optional::stream)
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .collect(Collectors.joining(" "))
                .toLowerCase(Locale.ROOT);

        if (value.isBlank()) {
            return Optional.empty();
        }
        if (containsAny(value, "cernet", "china education and research network")) {
            return Optional.of("教育网");
        }
        if (containsAny(value, "china broadnet", "china broadcasting", "broadcasting network", "cbn")) {
            return Optional.of("广电");
        }
        if (containsAny(value, "china mobile", "chinamobile", "cmcc", "cmnet")) {
            return Optional.of("移动");
        }
        if (containsAny(value, "china unicom", "unicom", "cncgroup", "china169")) {
            return Optional.of("联通");
        }
        if (containsAny(value, "china telecom", "chinanet", "ctnet", "cn2")) {
            return Optional.of("电信");
        }
        return rawIsp(root);
    }

    private Optional<String> rawIsp(JsonObject root) {
        return ispParts()
                .map(path -> jsonPathReader.read(root, path))
                .flatMap(Optional::stream)
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .map(this::stripAsnPrefix)
                .filter(part -> !part.isEmpty())
                .findFirst();
    }

    private Stream<String> ispParts() {
        return Stream.of("isp", "organization", "org", "asn_organization", "asname", "as");
    }

    private String stripAsnPrefix(String value) {
        return value.replaceFirst("(?i)^AS\\d+\\s+", "").trim();
    }

    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
