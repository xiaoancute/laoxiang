package com.xiaohunao.iplocationdisplay.display;

public final class DisplayTextFormatter {
    public String format(String displayFormat, String location) {
        String format = displayFormat == null || displayFormat.isBlank() ? "%location%" : displayFormat;
        String value = location == null ? "" : location;
        return format.replace("%location%", value).trim();
    }
}
