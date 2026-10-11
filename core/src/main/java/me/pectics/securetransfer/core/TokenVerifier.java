package me.pectics.securetransfer.core;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Verification;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Backend 侧:校验 transfer 凭据。
 * <p>{@code aud} 采用"可接受集合任一匹配"的语义(逐一归一化后比较),而非要求全部命中。</p>
 */
public final class TokenVerifier {

    private final Algorithm algorithm;
    private final String issuer;
    private final Set<String> audiences;
    private final Duration leeway;
    private final boolean requireIdentity;

    public TokenVerifier(
            final Algorithm algorithm,
            final String issuer,
            final List<String> audiences,
            final Duration leeway,
            final boolean requireIdentity) {
        this.algorithm = algorithm;
        this.issuer = issuer;
        this.audiences = new HashSet<>();
        if (audiences != null) {
            for (final String audience : audiences) {
                AudienceUtil.normalize(audience).ifPresent(this.audiences::add);
            }
        }
        this.leeway = leeway;
        this.requireIdentity = requireIdentity;
    }

    public DecodedJWT verify(final String token, final UUID expectedPlayerId) throws TokenVerificationException {
        final DecodedJWT jwt;
        try {
            Verification verification = JWT.require(this.algorithm);
            if (this.issuer != null && !this.issuer.isBlank()) {
                verification = verification.withIssuer(this.issuer);
            }
            if (!this.leeway.isZero()) {
                verification = verification.acceptLeeway(this.leeway.toSeconds());
            }
            final JWTVerifier verifier = verification.build();
            jwt = verifier.verify(token);
        } catch (final JWTVerificationException e) {
            throw new TokenVerificationException("JWT 无效: " + e.getMessage(), e);
        }

        if (!this.audiences.isEmpty()) {
            boolean matched = false;
            final List<String> tokenAudience = jwt.getAudience();
            if (tokenAudience != null) {
                for (final String candidate : tokenAudience) {
                    if (AudienceUtil.normalize(candidate)
                            .map(this.audiences::contains)
                            .orElse(false)) {
                        matched = true;
                        break;
                    }
                }
            }
            if (!matched) {
                throw new TokenVerificationException("audience 与期望不匹配");
            }
        }

        if (this.requireIdentity && expectedPlayerId != null) {
            final String subject = jwt.getSubject();
            if (subject == null || !subject.equalsIgnoreCase(expectedPlayerId.toString())) {
                throw new TokenVerificationException("sub 与登录玩家不匹配");
            }
        }

        return jwt;
    }

    public static String jwtId(final DecodedJWT jwt) {
        return jwt.getId();
    }

    public static Instant expiresAt(final DecodedJWT jwt) {
        return jwt.getExpiresAtAsInstant();
    }
}
