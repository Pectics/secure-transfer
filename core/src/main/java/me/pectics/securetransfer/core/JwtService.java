package me.pectics.securetransfer.core;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Gateway 侧:构造并签名短命的 transfer 凭据。
 */
public final class JwtService {

    private final Algorithm algorithm;
    private final String issuer;
    private final Duration ttl;

    public JwtService(final Algorithm algorithm, final String issuer, final Duration ttl) {
        this.algorithm = algorithm;
        this.issuer = issuer;
        this.ttl = ttl;
    }

    /**
     * @param playerId 玩家 UUID,写入 {@code sub}(身份绑定)
     * @param playerName 玩家名,写入 {@code name}
     * @param audience 目标后端(归一化后的 {@code host:port});可为 {@code null} 表示不绑定
     * @return 签名后的 JWT
     */
    public String sign(final UUID playerId, final String playerName, final String audience) {
        final Instant now = Instant.now();
        final JWTCreator.Builder builder = JWT.create()
                .withSubject(playerId.toString())
                .withClaim("name", playerName)
                .withIssuedAt(now)
                .withExpiresAt(now.plus(this.ttl))
                .withJWTId(UUID.randomUUID().toString());
        if (this.issuer != null && !this.issuer.isBlank()) {
            builder.withIssuer(this.issuer);
        }
        if (audience != null && !audience.isBlank()) {
            builder.withAudience(audience);
        }
        return builder.sign(this.algorithm);
    }
}
