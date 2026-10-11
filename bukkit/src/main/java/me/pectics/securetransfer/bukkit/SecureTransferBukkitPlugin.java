package me.pectics.securetransfer.bukkit;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import java.io.InputStream;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import me.pectics.securetransfer.core.JwtAlgorithms;
import me.pectics.securetransfer.core.ReplayStore;
import me.pectics.securetransfer.core.SqliteReplayStore;
import me.pectics.securetransfer.core.TokenVerifier;
import me.pectics.securetransfer.core.config.BackendConfig;
import me.pectics.securetransfer.core.config.ConfigLoader;
import org.bukkit.plugin.java.JavaPlugin;

public final class SecureTransferBukkitPlugin extends JavaPlugin {

    private BackendConfig config;
    private ReplayStore replayStore;
    private ReplayPruner pruner;

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        final Path configPath = this.getDataFolder().toPath().resolve("config.yml");
        this.config = ConfigLoader.load(configPath, BackendConfig.class, this.defaultConfig());
        if (!this.config.enabled) {
            this.getLogger().info("SecureTransfer (Backend) 已禁用。");
            return;
        }

        final TokenVerifier verifier = new TokenVerifier(
                JwtAlgorithms.fromConfig(this.config.jwt.algorithm, this.config.jwt.secret),
                this.config.jwt.issuer,
                this.config.jwt.audience,
                Duration.ofSeconds(this.config.jwt.leewaySeconds),
                this.config.jwt.requireIdentity);

        if (this.config.replay.enabled) {
            try {
                this.replayStore = new SqliteReplayStore(
                        this.getDataFolder().toPath().resolve(this.config.replay.database),
                        this.config.replay.busyTimeoutMs);
            } catch (final SQLException e) {
                this.getLogger().severe("初始化防重放 SQLite 失败,后端拒绝启动: " + e.getMessage());
                this.getServer().getPluginManager().disablePlugin(this);
                return;
            }
            this.pruner = new ReplayPruner(this, this.replayStore, this.config.replay.pruneIntervalSeconds);
            this.pruner.start();
        }

        final LoginCookieListener cookieListener = new LoginCookieListener(this.config);
        PacketEvents.getAPI().getEventManager().registerListener(cookieListener, PacketListenerPriority.NORMAL);

        PacketEvents.getAPI().init();

        this.getServer()
                .getPluginManager()
                .registerEvents(new PreLoginVerifier(this.config, verifier, this.replayStore, cookieListener), this);

        this.getLogger()
                .info(String.format(
                        "SecureTransfer (Backend) 已启用: cookie=%s, 算法=%s, replay=%s",
                        this.config.cookie.key, this.config.jwt.algorithm, this.config.replay.enabled));
    }

    @Override
    public void onDisable() {
        if (this.pruner != null) {
            this.pruner.stop();
        }
        if (this.replayStore != null) {
            this.replayStore.close();
        }
        PacketEvents.getAPI().terminate();
    }

    private InputStream defaultConfig() {
        return this.getResource("config.yml");
    }
}
