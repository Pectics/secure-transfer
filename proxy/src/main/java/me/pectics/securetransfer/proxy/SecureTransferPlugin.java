package me.pectics.securetransfer.proxy;

import java.io.InputStream;
import java.time.Duration;
import me.pectics.securetransfer.core.JwtAlgorithms;
import me.pectics.securetransfer.core.JwtService;
import me.pectics.securetransfer.core.config.ConfigLoader;
import me.pectics.securetransfer.core.config.GatewayConfig;
import net.transferproxy.api.event.EventType;
import net.transferproxy.api.event.listener.PreTransferListener;
import net.transferproxy.api.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SecureTransferPlugin implements Plugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecureTransferPlugin.class);

    @Override
    public void onEnable() {
        final GatewayConfig config = ConfigLoader.load(this.getConfigPath(), GatewayConfig.class, this.defaultConfig());
        if (!config.enabled) {
            LOGGER.info("SecureTransfer (Gateway) 已禁用。");
            return;
        }

        final JwtService jwtService = new JwtService(
                JwtAlgorithms.fromConfig(config.jwt.algorithm, config.jwt.secret),
                config.jwt.issuer,
                Duration.ofSeconds(config.jwt.ttlSeconds));

        this.getEventManager()
                .<PreTransferListener>addListener(EventType.PRE_TRANSFER, new PreTransferHandler(config, jwtService));

        LOGGER.info(
                "SecureTransfer (Gateway) 已启用: cookie={}, 算法={}, ttl={}s, bind-audience={}",
                config.cookie.key,
                config.jwt.algorithm,
                config.jwt.ttlSeconds,
                config.jwt.bindAudience);
    }

    private InputStream defaultConfig() {
        return SecureTransferPlugin.class.getClassLoader().getResourceAsStream("config.yml");
    }
}
