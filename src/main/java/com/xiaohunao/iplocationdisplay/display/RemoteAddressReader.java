package com.xiaohunao.iplocationdisplay.display;

import java.lang.reflect.Method;
import java.net.SocketAddress;

public final class RemoteAddressReader {
    public String read(Object packetListener) {
        if (packetListener == null) {
            return "";
        }

        SocketAddress directAddress = invokeSocketAddress(packetListener, "getConnectionAddress");
        if (directAddress != null) {
            return String.valueOf(directAddress);
        }

        try {
            Method getConnection = packetListener.getClass().getMethod("getConnection");
            Object connection = getConnection.invoke(packetListener);
            SocketAddress remoteAddress = invokeSocketAddress(connection, "getRemoteAddress");
            return remoteAddress == null ? "" : String.valueOf(remoteAddress);
        } catch (Exception ignored) {
            return "";
        }
    }

    private SocketAddress invokeSocketAddress(Object target, String methodName) {
        if (target == null) {
            return null;
        }

        try {
            Method method = target.getClass().getMethod(methodName);
            Object value = method.invoke(target);
            return value instanceof SocketAddress socketAddress ? socketAddress : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
