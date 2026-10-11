package me.pectics.securetransfer.bukkit;

import com.auth0.jwt.interfaces.DecodedJWT;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import me.pectics.securetransfer.core.ReplayStore;
import me.pectics.securetransfer.core.TokenVerificationException;
import me.pectics.securetransfer.core.TokenVerifier;
import me.pectics.securetransfer.core.config.BackendConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 唯一的判定关卡(方案 A):在 UUID 已解析的 {@code AsyncPlayerPreLoginEvent} 上,
 * 阻塞取回 packet 层捕获的 cookie(带超时),再完成验签 + 防重放 + 身份绑定。
 * <p>不通过 → {@code disallow},客户端停在"正在连接"界面被拒,到不了 configuration / 世界。</p>
 */
public final class PreLoginVerifier implements Listener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreLoginVerifier.class);

    private final BackendConfig config;
    private final TokenVerifier verifier;
    private final ReplayStore replayStore;
    private final LoginCookieListener cookieListener;

    public PreLoginVerifier(
            final BackendConfig config,
            final TokenVerifier verifier,
            final ReplayStore replayStore,
            final LoginCookieListener cookieListener) {
        this.config = config;
        this.verifier = verifier;
        this.replayStore = replayStore;
        this.cookieListener = cookieListener;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(final AsyncPlayerPreLoginEvent event) {
        if (this.isBypassed(event.getName())) {
            return;
        }

        final byte[] payload = this.awaitCookie(event.getName());
        if (payload == null || payload.length == 0) {
            this.reject(event, "未携带 cookie(疑似直连)或读取超时");
            return;
        }

        try {
            final DecodedJWT jwt =
                    this.verifier.verify(new String(payload, StandardCharsets.UTF_8), event.getUniqueId());
            if (this.replayStore != null
                    && !this.replayStore.recordIfAbsent(TokenVerifier.jwtId(jwt), TokenVerifier.expiresAt(jwt))) {
                this.reject(event, "检测到重放: jti=" + TokenVerifier.jwtId(jwt));
                return;
            }
        } catch (final TokenVerificationException e) {
            this.reject(event, e.getMessage());
        }
    }

    private byte[] awaitCookie(final String name) {
        final CompletableFuture<byte[]> future = this.cookieListener.take(name);
        try {
            return future.get(this.config.cookie.timeoutMs, TimeUnit.MILLISECONDS);
        } catch (final TimeoutException e) {
            return null;
        } catch (final Exception e) {
            return null;
        }
    }

    private boolean isBypassed(final String name) {
        return this.config.bypass.allowDirect
                || this.config.bypass.players.stream().anyMatch(p -> p.equalsIgnoreCase(name));
    }

    private void reject(final AsyncPlayerPreLoginEvent event, final String reason) {
        // 对外只给统一的 kick 文案,避免泄露具体校验细节。
        final Component message = MiniMessage.miniMessage().deserialize(this.config.onFailure.kickMessage);
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, message);
        LOGGER.info("拒绝 {} ({}): {}", event.getName(), event.getAddress(), reason);
    }
}
