package me.pectics.securetransfer.core.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import edu.umd.cs.findbugs.annotations.SuppressMatchType;
import java.util.ArrayList;
import java.util.List;

/** Backend(Bukkit / Fabric 侧)配置模型,两平台共用同一 schema。 */
@SuppressFBWarnings(
        value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
        justification = "公开字段属于 YAML 配置契约，由 Jackson 绑定并供平台模块消费，core 独立扫描无法完整判断读取情况。",
        matchType = SuppressMatchType.EXACT)
public final class BackendConfig {

    public boolean enabled = true;
    public Jwt jwt = new Jwt();
    public Cookie cookie = new Cookie();
    public Replay replay = new Replay();
    public OnFailure onFailure = new OnFailure();
    public Bypass bypass = new Bypass();
    public Log log = new Log();

    public static final class Jwt {
        public String algorithm = "HS512";
        public String secret = "";
        public String issuer = "transferproxy";
        public List<String> audience = new ArrayList<>();
        public int leewaySeconds = 5;
        public boolean requireIdentity = true;
    }

    public static final class Cookie {
        public String key = "securetransfer:token";
        /** 等待客户端回传 cookie 的超时(毫秒);超时视为失败。 */
        public int timeoutMs = 3000;
    }

    public static final class Replay {
        public boolean enabled = true;
        public String database = "used-tokens.db";
        public String synchronous = "FULL";
        public int busyTimeoutMs = 3000;
        public int pruneIntervalSeconds = 300;
    }

    public static final class OnFailure {
        public String kickMessage = "<red>Please connect through the gateway.</red>";
    }

    public static final class Bypass {
        public boolean allowDirect = false;
        public List<String> players = new ArrayList<>();
    }

    public static final class Log {
        public String level = "INFO";
    }
}
