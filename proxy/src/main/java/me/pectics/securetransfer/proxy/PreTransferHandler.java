package me.pectics.securetransfer.proxy;

import java.nio.charset.StandardCharsets;
import me.pectics.securetransfer.core.AudienceUtil;
import me.pectics.securetransfer.core.JwtService;
import me.pectics.securetransfer.core.config.GatewayConfig;
import net.transferproxy.api.event.listener.PreTransferListener;
import net.transferproxy.api.event.transfer.PreTransferEvent;
import net.transferproxy.api.network.connection.PlayerConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 在 transfer packet 发出前,为玩家签发并写入 transfer 凭据 cookie。
 * <p>挂 {@code PRE_TRANSFER}(在请求 transfer 的线程上同步触发),因此天然先于 transfer,
 * 无需与路由插件争抢 READY 顺序。</p>
 */
public final class PreTransferHandler implements PreTransferListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreTransferHandler.class);

    private final GatewayConfig config;
    private final JwtService jwtService;

    public PreTransferHandler(final GatewayConfig config, final JwtService jwtService) {
        this.config = config;
        this.jwtService = jwtService;
    }

    @Override
    public void handle(final PreTransferEvent event) {
        final PlayerConnection connection = event.getConnection();
        final String audience = resolveAudience(event);
        final String token = this.jwtService.sign(connection.getUUID(), connection.getName(), audience);
        connection.storeCookie(this.config.cookie.key, token.getBytes(StandardCharsets.UTF_8));
        LOGGER.debug("已为 {} 签发 token(aud={})并写入 cookie {}", connection.getName(), audience, this.config.cookie.key);
    }

    private String resolveAudience(final PreTransferEvent event) {
        if (!this.config.jwt.bindAudience) {
            return null;
        }
        final String raw = event.getHost() + ":" + event.getHostPort();
        return AudienceUtil.normalize(raw).orElse(null);
    }
}
