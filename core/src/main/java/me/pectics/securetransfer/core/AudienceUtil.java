package me.pectics.securetransfer.core;

import java.util.Locale;
import java.util.Optional;

/**
 * 归一化 {@code host[:port]} 目标,供<b>签发端与校验端共用同一规则</b>,
 * 从而避免上下游因大小写 / 端口写法不一致而错配。
 * <ul>
 *   <li>去首尾空白,host 转小写,去掉单个尾点(FQDN 根点);</li>
 *   <li>缺省端口补 {@value SecureTransfer#DEFAULT_MINECRAFT_PORT};</li>
 *   <li>IPv6 保留方括号,形如 {@code [::1]:25565}。</li>
 * </ul>
 */
public final class AudienceUtil {

    private AudienceUtil() {}

    public static Optional<String> normalize(final String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        final String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }

        String host;
        int port;
        if (trimmed.startsWith("[")) {
            final int close = trimmed.indexOf(']');
            if (close < 0) {
                return Optional.empty();
            }
            host = trimmed.substring(0, close + 1);
            final String rest = trimmed.substring(close + 1);
            port = parsePort(rest.startsWith(":") ? rest.substring(1) : "");
        } else {
            final int colon = trimmed.lastIndexOf(':');
            if (colon < 0) {
                host = trimmed;
                port = SecureTransfer.DEFAULT_MINECRAFT_PORT;
            } else {
                host = trimmed.substring(0, colon);
                port = parsePort(trimmed.substring(colon + 1));
            }
        }

        host = host.toLowerCase(Locale.ROOT);
        if (host.length() > 1 && host.endsWith(".")) {
            host = host.substring(0, host.length() - 1);
        }
        if (host.isEmpty() || port <= 0 || port > 65535) {
            return Optional.empty();
        }
        return Optional.of(host + ":" + port);
    }

    private static int parsePort(final String value) {
        if (value == null || value.isEmpty()) {
            return SecureTransfer.DEFAULT_MINECRAFT_PORT;
        }
        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException e) {
            return -1;
        }
    }
}
