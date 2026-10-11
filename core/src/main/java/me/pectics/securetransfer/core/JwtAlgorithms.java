package me.pectics.securetransfer.core;

import com.auth0.jwt.algorithms.Algorithm;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;

/**
 * 由配置构造对称(HMAC)签名算法。
 * <p>密钥优先按 Base64 解码(推荐 {@code openssl rand -base64 64});解码失败时按原始 UTF-8 字节处理。</p>
 */
public final class JwtAlgorithms {

    private JwtAlgorithms() {}

    public static Algorithm fromConfig(final String algorithmName, final String secret) {
        final byte[] key = decodeSecret(secret);
        return switch (algorithmName == null ? "HS512" : algorithmName.toUpperCase(Locale.ROOT)) {
            case "HS256" -> Algorithm.HMAC256(key);
            case "HS384" -> Algorithm.HMAC384(key);
            case "HS512" -> Algorithm.HMAC512(key);
            default -> throw new IllegalArgumentException("不支持的 JWT 算法: " + algorithmName);
        };
    }

    private static byte[] decodeSecret(final String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("jwt.secret 未配置");
        }
        try {
            return Base64.getDecoder().decode(secret);
        } catch (final IllegalArgumentException e) {
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }
}
