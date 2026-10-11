package me.pectics.securetransfer.core;

import java.time.Instant;

/**
 * 防重放:记录已消费过的 JWT ID({@code jti})。
 */
public interface ReplayStore extends AutoCloseable {

    /**
     * 原子记录 {@code jwtId}。
     *
     * @return {@code true} 表示首次出现(可用);{@code false} 表示已存在(重放)。
     *         实现必须保证在返回 {@code true} 之前该记录已<b>持久化提交(fsync)</b> ——
     *         否则进程/机器崩溃会引入重放窗口。
     */
    boolean recordIfAbsent(String jwtId, Instant expiresAt);

    /** 清理已过期记录。 */
    void prune(Instant now);

    @Override
    void close();
}
