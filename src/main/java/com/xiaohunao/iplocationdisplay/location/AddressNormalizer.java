package com.xiaohunao.iplocationdisplay.location;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

public final class AddressNormalizer {
    public Optional<String> normalize(String rawAddress) {
        if (rawAddress == null) {
            return Optional.empty();
        }

        String value = rawAddress.trim();
        if (value.isEmpty()) {
            return Optional.empty();
        }

        if (value.startsWith("/")) {
            value = value.substring(1);
        }

        if (value.startsWith("[")) {
            int endBracket = value.indexOf(']');
            if (endBracket > 1) {
                value = value.substring(1, endBracket);
            }
        } else if (looksLikeIpv4WithPort(value)) {
            value = value.substring(0, value.lastIndexOf(':'));
        }

        return isValidIp(value) ? Optional.of(value) : Optional.empty();
    }

    public boolean isLocalOrPrivate(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            return address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress()
                    || isUniqueLocalIpv6(address);
        } catch (UnknownHostException ignored) {
            return false;
        }
    }

    private boolean looksLikeIpv4WithPort(String value) {
        int colon = value.lastIndexOf(':');
        return colon > 0 && value.indexOf(':') == colon && value.indexOf('.') >= 0;
    }

    private boolean isValidIp(String value) {
        try {
            InetAddress address = InetAddress.getByName(value);
            return (address instanceof Inet4Address || address instanceof Inet6Address)
                    && !value.chars().anyMatch(Character::isWhitespace);
        } catch (UnknownHostException ignored) {
            return false;
        }
    }

    private boolean isUniqueLocalIpv6(InetAddress address) {
        if (!(address instanceof Inet6Address)) {
            return false;
        }
        byte firstByte = address.getAddress()[0];
        return (firstByte & 0xfe) == 0xfc;
    }
}
