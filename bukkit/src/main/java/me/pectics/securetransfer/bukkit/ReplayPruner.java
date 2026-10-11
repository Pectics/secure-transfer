package me.pectics.securetransfer.bukkit;

import java.time.Instant;
import me.pectics.securetransfer.core.ReplayStore;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/** 周期性清理 SQLite 中已过期的 jti 记录。 */
final class ReplayPruner {

    private final Plugin plugin;
    private final ReplayStore store;
    private final long intervalSeconds;
    private BukkitTask task;

    ReplayPruner(final Plugin plugin, final ReplayStore store, final long intervalSeconds) {
        this.plugin = plugin;
        this.store = store;
        this.intervalSeconds = Math.max(1L, intervalSeconds);
    }

    void start() {
        final long period = this.intervalSeconds * 20L;
        this.task = this.plugin
                .getServer()
                .getScheduler()
                .runTaskTimerAsynchronously(this.plugin, this::prune, period, period);
    }

    void stop() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
    }

    private void prune() {
        try {
            this.store.prune(Instant.now());
        } catch (final RuntimeException e) {
            this.plugin.getLogger().warning("清理过期 jti 失败: " + e.getMessage());
        }
    }
}
