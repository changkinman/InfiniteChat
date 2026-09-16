# 一致性哈希动态路由设计

## 1. 背景与目标

InfiniteChat 当前通过 Spring Cloud Gateway 将 WebSocket 握手请求负载均衡到 Nacos 中注册的 `NettyService` 实例。连接建立后，RTC 节点把 `user:session:{userId}` 写入 Redis，消息服务和联系人服务据此调用承载该用户连接的 RTC 节点。

现有方案尚未控制同一用户的节点归属，也无法可靠处理节点变化后的跨节点重复连接、RTC 异常退出造成的陈旧路由，以及旧连接误删新连接路由等问题。

本次实现一个最小可运行的一致性哈希路由版本，目标如下：

- WebSocket 建连时，以请求头 `userUuid` 为 key，通过一致性哈希选择 Nacos 中的 `NettyService` 实例。
- 节点列表变化时只替换 Gateway 中的哈希环快照，不迁移已有长连接；新连接和重连使用新环。
- RTC 在 Redis 中维护用户当前实际连接节点及连接所有权，后续推送直接查询 Redis 精准路由。
- 同一用户正常情况下只保留一条连接；跨节点重连时通知旧节点关闭旧连接。
- 使用 TTL、条件续期和条件删除，避免节点异常退出留下永久路由以及旧连接删除新路由。
- 不新增一致性哈希或测试类第三方依赖。

## 2. 范围

### 2.1 本次包含

- Gateway 对 WebSocket 路由的 `userUuid` 校验。
- 仅绑定到 `NettyService` 的一致性哈希 LoadBalancer。
- 从 Nacos 动态获取 Netty 节点并按实例集合变化重建哈希环。
- RTC 连接唯一标识、本地连接管理和 Redis 路由所有权管理。
- Redis Lua 原子抢占、匹配续期和匹配删除。
- 跨节点旧连接踢线接口与调用客户端。
- 消息服务、联系人服务适配新的 Redis Hash 路由记录。
- 单元测试、相关 Maven 模块测试与编译、手工多节点联调说明。

### 2.2 本次不包含

- 多设备、多端同时在线。
- 按 Nacos 权重分配连接。
- 已有 WebSocket 连接的主动迁移。
- 分布式锁或网络分区下的强一致单连接。
- 踢线失败重试队列、熔断器或补偿任务。
- 群聊及朋友圈广播链路的按节点聚合优化。
- 新增监控面板或独立路由服务。
- 真实 Nacos、Redis 多节点环境的自动化端到端编排。

## 3. 已确认的设计决策

- 一致性哈希只用于 WebSocket 建连选址；建连后的推送以 Redis 实际路由为准。
- 节点发现使用现有 Nacos `NettyService`，Redis 不承担节点注册或发现。
- 哈希 key 使用 WebSocket 握手请求头 `userUuid`；JWT 仍由 RTC 校验。
- 缺少或空白 `userUuid` 时，Gateway 返回 HTTP 400，不退化为随机路由。
- 每个物理节点等权，默认创建 128 个虚拟节点，数量可配置。
- 单用户单连接；跨节点踢线通过内部 HTTP 完成。
- 新连接先取得 Redis 路由所有权。旧节点不可达时保留新连接，由系统最终收敛。
- Redis 路由 TTL 默认 10 分钟且可配置，客户端心跳负责续期。
- 验收以自动化单元测试、模块测试和编译为主；真实基础设施通过手工步骤验证。

## 4. 总体架构

```text
Client
  -> Gateway WebSocketUserIdFilter
  -> ConsistentHashLoadBalancer
  -> Nacos NettyService instance
  -> RTC JWT validation
  -> Redis route ownership claim
  -> local Channel registration

Business service
  -> Redis user route lookup
  -> exact RTC HTTP endpoint
  -> local Channel push
```

Gateway 负责“新连接应该去哪里”，Redis 负责“用户当前实际连在哪里”。一致性哈希结果不是在线状态，也不能替代 Redis 路由。节点加入或离开只改变之后的建连结果；仍然存活的既有连接继续由原节点承载。

## 5. Gateway 设计

### 5.1 请求校验

新增 `WebSocketUserIdFilter`，仅匹配 `/api/v1/netty`：

- 读取 `userUuid` 请求头。
- 请求头不存在或去除空白后为空时，立即返回 400。
- 校验通过后不修改认证信息，继续交给 Gateway 路由。

该过滤器只做路由前置条件校验，不解析 JWT。RTC 仍比较 JWT subject 与 `userUuid`，防止伪造身份建立连接。

### 5.2 一致性哈希 LoadBalancer

新增实现 Spring Cloud LoadBalancer 扩展点的 `ConsistentHashLoadBalancer`，并通过专用配置只绑定到 `NettyService`。其他 HTTP 微服务继续使用原有负载均衡策略，现有路由 `lb:ws://NettyService` 保持不变。

LoadBalancer 从请求上下文读取 `userUuid`，从 `ServiceInstanceListSupplier` 取得 Nacos 实例列表。节点身份定义为 `host:port`；输入实例先按节点身份排序，因此 Nacos 返回顺序不会影响环结果。

哈希环规则：

- 使用 JDK `MessageDigest` 提供的 SHA-256，不增加外部依赖。
- 每个物理节点默认生成 128 个虚拟节点。
- 虚拟节点 key 为 `nodeId + "#" + replicaIndex`。
- 用户位置为 `SHA-256(userUuid)`。
- 在有序环上选择第一个大于等于用户位置的虚拟节点；不存在时回绕到首节点。

实例集合以排序后的节点身份列表生成签名。签名未变化时复用当前不可变环；变化时先完整构建新环，再以原子引用一次性替换。并发请求不会看到半构建状态。

没有可用实例时返回空的 LoadBalancer 响应，由 Gateway 转换为 503。

## 6. Redis 在线路由模型

每个用户使用一个 Redis Hash：

```text
key: user:session:{userId}
fields:
  nodeId        = Netty 节点稳定身份
  endpoint      = RTC 节点内部 HTTP 基础地址
  connectionId  = 本次 WebSocket 连接的唯一标识
ttl: 默认 10 分钟
```

`endpoint` 默认由本机可达地址和 `server.port` 组成，并允许配置显式覆盖，以适配容器或代理环境。业务服务读取 `endpoint` 后拼接现有推送路径，不再假定 Redis value 是裸 IP。

`connectionId` 在握手认证通过后生成，并同时绑定到 Netty Channel。所有清理、续期和踢线操作都必须携带它，不能只按 `userId` 操作。

现网旧版本把同名 Key 保存为 Redis String，而新版本使用 Redis Hash，两者不能并存。本次 MVP 采用停机升级：部署所有相关服务前清理临时在线路由 `user:session:*`，完成部署后由客户端重连重建 Hash 路由；不支持新旧版本滚动共存。该前缀只存放可重建的在线连接状态，不得清理其他业务 Key。

### 6.1 原子抢占

Lua 脚本在一次执行中：

1. 读取并返回旧路由的 `nodeId`、`endpoint` 和 `connectionId`。
2. 写入新路由的三个字段。
3. 设置完整 TTL。

新连接写入后成为 Redis 中的权威连接。脚本返回的旧路由用于本地或跨节点踢线。

### 6.2 条件续期

心跳续期脚本仅在 Hash 中的 `connectionId` 等于当前 Channel 持有的标识时执行 `EXPIRE`。已经失去所有权的旧连接无法延长新路由或自身的存活状态。

### 6.3 条件删除

断线清理脚本仅在 Hash 中的 `connectionId` 等于当前 Channel 持有的标识时删除整个 Key：

```lua
if redis.call('HGET', key, 'connectionId') == expectedConnectionId then
    return redis.call('DEL', key)
end
return 0
```

因此旧连接延迟触发 `channelInactive` 时，不会删除后来连接写入的路由。

## 7. RTC 组件设计

### 7.1 `OnlineRouteRegistry`

统一封装：

- `claim(userId, route)`：原子抢占并返回旧路由。
- `isOwner(userId, connectionId)`：确认当前连接是否仍拥有路由。
- `renew(userId, connectionId)`：匹配后续期。
- `release(userId, connectionId)`：匹配后删除。

业务代码不得绕过该组件直接修改在线路由。

### 7.2 `ChannelManager`

本地映射的值从裸 `Channel` 提升为包含 `userId`、`connectionId` 和 `Channel` 的连接绑定。管理器提供按用户注册、按连接标识匹配关闭、反向查询和安全移除能力。

内部踢线必须同时匹配 `userId` 和 `connectionId`。重复请求、过期请求或目标连接已经关闭时均幂等成功，不得关闭后来建立的连接。

### 7.3 `ConnectionTakeoverClient`

当抢占脚本返回的旧路由不为空时：

- 旧连接位于当前节点：直接按旧 `connectionId` 关闭本地 Channel。
- 旧连接位于其他节点：调用旧 `endpoint` 上的内部踢线接口，并携带 `userId` 与旧 `connectionId`。
- 请求使用较短的连接和读取超时，失败只记录告警，不回滚新连接。

内部踢线接口不挂载到 Gateway 的公开路由前缀，仅用于服务网络内节点互调。本次 MVP 不新增独立的服务间认证机制。

## 8. 连接生命周期与并发语义

### 8.1 建连

1. RTC 完成 WebSocket 升级事件处理并校验 JWT。
2. 生成唯一 `connectionId`，构造本节点路由。
3. 通过 `OnlineRouteRegistry.claim` 抢占 Redis 所有权并取得旧路由。
4. 在 `ChannelManager` 注册新连接。
5. 再次调用 `isOwner`；如果所有权已被另一个并发连接抢走，移除并关闭当前连接。
6. 仍为所有者时，根据旧路由关闭本地旧连接或通知旧节点踢线。

步骤 5 处理以下竞态：节点 B 在节点 A 完成 Redis 抢占后立即再次抢占，但 B 的踢线请求到达 A 时，A 尚未注册本地 Channel。A 注册后复查失败，便主动关闭自己。

### 8.2 心跳

收到合法心跳后，RTC 先执行匹配续期。续期成功才返回心跳响应；续期失败表示当前连接已失去权威所有权，RTC 关闭该连接。

### 8.3 下线与异常关闭

RTC 先按 `connectionId` 安全移除本地绑定，再执行 Redis 条件删除，最后关闭 Channel。无用户绑定的未认证连接只关闭 Channel，不访问 `user:session:null` 等无效 Key。

### 8.4 单连接保证

正常网络和 Redis 可用时，新连接会关闭旧连接。网络分区或旧节点不可达时，物理旧连接可能短暂存在，但 Redis 只承认最新 `connectionId`；路由推送只到新节点，旧连接不能续期权威路由，并最终通过空闲检测或节点恢复后的处理收敛。

本次不宣称网络分区下的强一致单连接。

## 9. 错误处理

- 缺少 `userUuid`：Gateway 返回 400。
- Nacos 无 `NettyService` 实例：Gateway 返回 503。
- JWT 无效或与 `userUuid` 不一致：RTC 关闭连接且不写 Redis。
- Redis 抢占失败：RTC 关闭新连接，因为无法保证后续精准路由。
- Redis 心跳续期失败：视为失去所有权并关闭连接。
- Redis 条件删除返回 0：表示路由已变更或不存在，按幂等结果处理。
- 内部踢线目标不存在或连接标识不匹配：幂等成功。
- 旧节点不可达或踢线超时：记录包含节点和用户上下文的告警，保留新连接。
- 哈希计算异常：记录错误并让本次选址失败，不退化为随机负载均衡。

日志不得记录 JWT、完整消息正文或其他敏感认证数据。

## 10. 配置

配置键名固定如下：

```yaml
infinitechat:
  routing:
    consistent-hash:
      virtual-nodes: 128
    online-route:
      ttl: 10m
      advertised-http-endpoint: "" # 为空时由本机地址与 server.port 推导
    takeover:
      connect-timeout: 1s
      read-timeout: 2s
```

`virtual-nodes` 必须大于 0，TTL 必须明显大于客户端正常心跳间隔。非法配置在应用启动时失败，而不是运行时静默回退。

## 11. 测试设计

### 11.1 Gateway

- 相同实例集合和 `userUuid` 始终选择同一节点。
- Nacos 实例返回顺序变化不改变选择结果。
- 新增或删除一个节点时，只有部分测试用户改变映射。
- 多节点、128 个虚拟节点下的样本分布无明显倾斜。
- 实例列表变化时生成新快照，未变化时复用快照。
- 缺失、空白 `userUuid` 返回 400。
- 空实例列表产生 503，不回退到随机节点。
- 专用 LoadBalancer 仅影响 `NettyService`。

### 11.2 RTC 与 Redis 路由

- 抢占返回旧路由并建立新所有权。
- 匹配的 `connectionId` 可以续期和删除。
- 不匹配的旧连接不能续期或删除新路由。
- Redis 抢占异常会关闭新连接。
- 并发抢占后复查失败的连接主动关闭。
- 本地旧连接和跨节点旧连接均按精确 `connectionId` 踢线。
- 重复踢线和过期踢线保持幂等。
- 未认证连接关闭时不操作无效 Redis Key。

不新增 Redis 容器或嵌入式 Redis 测试依赖。自动化测试通过模拟 Redis 脚本执行结果验证 Java 状态转换、参数和异常分支；Lua 与真实 Redis 的原子行为在手工联调中验证。

### 11.3 兼容调用方

- 消息服务能从 Redis Hash 的 `endpoint` 构造现有单聊推送请求。
- 联系人服务能从相同字段构造好友申请和新会话推送请求。
- 用户离线或路由不存在时保持现有“不执行实时推送”的业务语义。

## 12. 手工联调场景

使用真实 Nacos、Redis、一个 Gateway 和至少两个 RTC 实例：

1. 使用多个 `userUuid` 建连，确认相同用户重复连接稳定命中同一节点。
2. 新增第三个 RTC 节点，确认已有连接不断开，只有部分后续重连改变节点。
3. 删除一个 RTC 节点，确认其用户重连后被分配到剩余节点。
4. 让同一用户在哈希环变化后重连到另一节点，确认旧节点收到精确踢线且 Redis 指向新连接。
5. 延迟触发旧 Channel 的下线回调，确认新路由未被删除。
6. 停止 RTC 而不执行优雅清理，确认对应路由在 TTL 后消失。
7. 模拟旧节点不可达，确认新连接仍然成功并成为 Redis 权威路由。
8. 从旧版本升级前停止相关服务并仅清理 `user:session:*` 在线路由，确认新版本重连后创建 Redis Hash，且不出现 `WRONGTYPE`。

## 13. 完成标准

- Gateway 的 WebSocket 建连已使用 Nacos 动态实例和一致性哈希选址。
- Redis 在线路由包含真实 RTC endpoint 与唯一连接所有权。
- 心跳续期、断线清理及跨节点踢线均按 `connectionId` 防止误操作。
- 相关自动化测试通过。
- Gateway、RTC、消息服务和联系人服务完成 Maven 测试与编译。
- README 或专门说明文档包含双节点手工联调步骤及已知 MVP 限制。
