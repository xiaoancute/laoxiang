package com.xiaohunao.iplocationdisplay.display;

public final class DisplayTextFormatter {
    public String format(String displayFormat, String location, String playtime) {
        String format = displayFormat == null || displayFormat.isBlank() ? "%location%" : displayFormat;
        String locationValue = location == null ? "" : location;
        String playtimeValue = playtime == null ? "" : playtime;
        return format
                .replace("%location%", locationValue)
                .replace("%playtime%", playtimeValue)
                .trim();
    }
}
