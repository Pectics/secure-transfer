# SecureTransfer — Fabric 后端模块(设计方向)

> **状态:未实现。** 本文记录方向、已核实的事实与风险,供后续动工。实现前请先读最后一节。

## 目标

在 Fabric 后端服务端上,读取 Gateway 在 `/transfer` 前写入的 transfer cookie(JWT),
验签 + 防重放,**拒绝未携带有效 cookie 的直连玩家**。职责与 `bukkit` 模块一致,复用 `core`。

## 关键约束:Fabric 没有 cookie API

- 曾计划使用的 `ServerCookieStore` / `ServerTransferable` 来自 **PR #3547**,该 PR 状态为
  **closed / 未合并**。在 FabricMC 组织内代码搜索 `CookieStore`、`ServerCookieStore` 均为 **0 结果**。
- 结论:**Fabric API 目前没有任何 cookie 读取能力**,不能像原先设想那样"干净地读 cookie"。

## 两条可选路线

| 路线 | 做法 | 评价 |
|---|---|---|
| **A. PacketEvents-Fabric**(推荐) | 引入 PacketEvents 的 Fabric 平台,**复用与 `bukkit` 完全相同的 `LoginCookieListener` 与校验逻辑** | 与 bukkit 同构,代码几乎零重复;代价是引入一个 mod 依赖 |
| B. Mixin | mixin `ServerLoginPacketListenerImpl`:登录时发 `ClientboundCookieRequestPacket`、拦 `ServerboundCookieResponsePacket` | 无额外依赖;但需 access widener、依赖 MC 内部类名,**随版本极易碎**,工程量更大 |

**方向:先做 A;若 PacketEvents-Fabric 在 login 阶段不覆盖或不稳定,再退到 B。**

## 架构与复用(与 bukkit 一致的方案 A)

- **复用 `core`**:`JwtService`、`TokenVerifier`、`AudienceUtil`、`ReplayStore`(`SqliteReplayStore`)、`BackendConfig`、`ConfigLoader`。
- **判定关卡方案 A**(已在 bukkit 落地,详见 `bukkit` 模块):
  1. packet 层**只捕获**:`LOGIN_START` → 发 `COOKIE_REQUEST`;`COOKIE_RESPONSE` → 按玩家名登记/完成 future(不判定)。
  2. **唯一判定**在"UUID 已解析"的时机:阻塞取回 future(带超时)→ 验签 + `aud` + 身份绑定 `sub == UUID` + 防重放 → 失败断开。
- Fabric 上"UUID 已解析的时机"对应**登录认证完成、玩家加入前**的网络处理器回调(等价于 bukkit 的
  `AsyncPlayerPreLoginEvent`)。**具体挂点待定**(A 路线下通常是 PacketEvents 的 login 阶段事件,
  或 Fabric API 的 `ServerLoginConnectionEvents`)。

## 版本与构建(已核实)

| 项 | 值 |
|---|---|
| Fabric Loader | `0.19.5` |
| Fabric API(MC 26.3) | `0.162.0+26.3`(注:**不含** cookie API) |
| PacketEvents Fabric | `com.github.retrooper:packetevents-fabric:2.14.0`(CodeMC 仓库) |
| yarn 映射(26.3) | **未发布** → 只能用 Mojang 官方映射 `loom.officialMojangMappings()` |
| Fabric Loom | 最新 `1.19.0-alpha.x`(与 Gradle 9.7.1 兼容性**未验证**) |
| Java toolchain | 25(与其他模块一致) |

构建骨架(Gradle,需 Loom 插件;仅为示意,未验证):

```gradle
plugins {
    id 'fabric-loom' version '<ver>'
}

dependencies {
    minecraft "com.mojang:minecraft:26.3"
    mappings loom.officialMojangMappings()
    modImplementation "net.fabricmc:fabric-loader:0.19.5"
    modImplementation "net.fabricmc.fabric-api:fabric-api:0.162.0+26.3"

    implementation project(':core')
    implementation 'org.xerial:sqlite-jdbc:3.46.1.3'
    // PacketEvents:按 Fabric mod 的方式引入(具体方式待定,jar-in-jar 或运行时 mod 依赖)
    modImplementation 'com.github.retrooper:packetevents-fabric:2.14.0'
}
```

Mod 入口:`net.fabricmc.api.ModInitializer`(`fabric.mod.json` 声明)。

## 待验证 / 风险(动工前务必确认)

1. **触发 Minecraft 下载**:Loom 首次构建会从 `libraries.minecraft.net` 拉取 MC 本体与依赖库(可能上百 MB)。**动工前需确认是否允许**。
2. **Loom × Gradle 9.7.1**:兼容性未验证;若不兼容,可能需降 Gradle 或等 Loom 更新。
3. **PacketEvents-Fabric 是否覆盖 login 阶段**:方案 A 成立的前提;需确认其服务端 login/dedicated server 支持。
4. **UUID 绑定挂点**:Fabric 上拿"已解析 UUID"的具体回调位置待定;若拿不到,退化为 `require-identity: false`(与 bukkit 早先的取舍相同)。
5. **跨侧 UUID 一致性**:Gateway 与 Backend 的 UUID 派生方式须一致(都正版 / 都离线),否则身份绑定天然对不上。
6. **携带方式**:PacketEvents / sqlite-jdbc 等依赖是 jar-in-jar 打进包,还是要求运行时安装,待定。

## 参考

- 本项目 `bukkit` 模块(方案 A 的参考实现)与 `core` 模块(共享契约)
- FabricMC/fabric PR #3547(Cookie Api,**已关闭未合并**)
- PacketEvents:`https://github.com/retrooper/packetevents`(含 `fabric` / `fabric-official` 平台)
