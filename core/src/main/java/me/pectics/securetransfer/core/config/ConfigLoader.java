package me.pectics.securetransfer.core.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 统一的 YAML 配置加载器(三平台共用),使 gateway / backend 的配置模型一致。
 */
public final class ConfigLoader {

    private static final ObjectMapper MAPPER =
            new ObjectMapper(new YAMLFactory()).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private ConfigLoader() {}

    /**
     * 读取 YAML 配置。文件不存在时,先把 {@code defaultResource} 写到目标路径再解析。
     *
     * @param file 目标配置文件路径
     * @param type 配置模型类型
     * @param defaultResource 默认配置资源的输入流(可为 {@code null});调用方负责关闭
     */
    public static <T> T load(final Path file, final Class<T> type, final InputStream defaultResource) {
        try {
            if (Files.notExists(file)) {
                final Path parent = file.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                if (defaultResource != null) {
                    Files.copy(defaultResource, file);
                }
            }
            try (InputStream in = Files.newInputStream(file)) {
                return MAPPER.readValue(in, type);
            }
        } catch (final IOException e) {
            throw new UncheckedIOException("加载配置失败: " + file, e);
        }
    }
}
