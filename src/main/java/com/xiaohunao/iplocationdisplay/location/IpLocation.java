package com.xiaohunao.iplocationdisplay.location;

public record IpLocation(String value) {
    public IpLocation {
        value = value == null ? "" : value.trim();
    }
}
