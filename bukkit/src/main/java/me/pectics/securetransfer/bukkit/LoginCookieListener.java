package me.pectics.securetransfer.bukkit;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientCookieResponse;
import com.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientLoginStart;
import com.github.retrooper.packetevents.wrapper.login.server.WrapperLoginServerCookieRequest;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import me.pectics.securetransfer.core.config.BackendConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 登录期"捕获"客户端回传的 cookie,不做判定。
 * <p>在 {@code LOGIN_START} 时按玩家名登记一个 future 并发出 {@code COOKIE_REQUEST};
 * 在 {@code COOKIE_RESPONSE} 时用 payload 完成该 future。判定交给
 * {@link PreLoginVerifier}(在 UUID 已解析的 {@code AsyncPlayerPreLoginEvent} 上做)。</p>
 */
public final class LoginCookieListener implements PacketListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginCookieListener.class);

    private final ResourceLocation cookieKey;
    private final Map<String, CompletableFuture<byte[]>> pending = new ConcurrentHashMap<>();

    public LoginCookieListener(final BackendConfig config) {
        this.cookieKey = new ResourceLocation(config.cookie.key);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Login.Client.LOGIN_START) {
            final String name = key(new WrapperLoginClientLoginStart(event).getUsername());
            if (name == null) {
                return;
            }
            this.pending.put(name, new CompletableFuture<>());
            event.getUser().sendPacket(new WrapperLoginServerCookieRequest(this.cookieKey));
        } else if (event.getPacketType() == PacketType.Login.Client.COOKIE_RESPONSE) {
            final WrapperLoginClientCookieResponse response = new WrapperLoginClientCookieResponse(event);
            if (!this.cookieKey.equals(response.getKey())) {
                return;
            }
            final String name = key(event.getUser().getName());
            final CompletableFuture<byte[]> future = name == null ? null : this.pending.get(name);
            if (future != null) {
                future.complete(response.getPayload());
            }
        }
    }

    @Override
    public void onUserDisconnect(final UserDisconnectEvent event) {
        final String name = key(event.getUser().getName());
        if (name != null) {
            final CompletableFuture<byte[]> future = this.pending.remove(name);
            if (future != null) {
                future.complete(null);
            }
        }
    }

    /** 取走并移除该玩家名对应的 cookie future;若不存在,返回已用 {@code null} 完成的 future。 */
    public CompletableFuture<byte[]> take(final String name) {
        final String k = key(name);
        final CompletableFuture<byte[]> future = k == null ? null : this.pending.remove(k);
        return future != null ? future : CompletableFuture.completedFuture(null);
    }

    private static String key(final String name) {
        return name == null ? null : name.toLowerCase(Locale.ROOT);
    }
}
