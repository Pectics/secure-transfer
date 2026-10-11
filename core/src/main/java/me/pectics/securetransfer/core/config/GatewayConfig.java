package me.pectics.securetransfer.core.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import edu.umd.cs.findbugs.annotations.SuppressMatchType;

/** Gateway(TransferProxy 侧)配置模型,对应 proxy 插件的 config.yml。 */
@SuppressFBWarnings(
        value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
        justification = "公开字段属于 YAML 配置契约，由 Jackson 绑定并供平台模块消费，core 独立扫描无法完整判断读取情况。",
        matchType = SuppressMatchType.EXACT)
public final class GatewayConfig {

    public boolean enabled = true;
    public Jwt jwt = new Jwt();
    public Cookie cookie = new Cookie();
    public Log log = new Log();

    public static final class Jwt {
        public String algorithm = "HS512";
        public String secret = "";
        public String issuer = "transferproxy";
        public int ttlSeconds = 30;
        public boolean bindAudience = true;
    }

    public static final class Cookie {
        public String key = "securetransfer:token";
    }

    public static final class Log {
        public String level = "INFO";
    }
}
