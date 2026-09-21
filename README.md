# InfiniteChat 2.0

InfiniteChat 2.0 是一个基于 Spring Cloud 的即时通信后端项目。项目将用户、实时通信、离线消息和红包等能力拆分为独立服务，并通过 Kafka、Redis、MySQL、Nacos 等组件协作。

## 项目能力

- 账号注册、邮箱验证码登录、密码登录、令牌刷新与登出
- 好友申请、好友管理、拉黑与解除拉黑
- 单聊、群聊、用户会话与群组创建/邀请
- Netty WebSocket 实时消息推送与心跳保活
- 在线消息推送、离线消息查询、历史消息查询
- 红包发送、领取、余额变动记录、过期退款
- Nacos 服务注册与发现、网关鉴权与统一请求转发

## 服务划分

| 模块 | 端口 | 职责 |
| --- | ---: | --- |
| `Common` | - | 公共响应、异常、DTO、常量、JWT、数据库与工具类，不单独启动。 |
| `UserService` | 8104 | 用户、认证、好友、群组、会话、头像上传及内部校验。 |
| `OfflineDataService` | 8101 | 消费并持久化消息，提供离线消息、历史消息和会话数据查询。 |
| `RealTimeService` | 8102 | 实时通信服务；其中 Netty WebSocket 独立监听 9101 端口。 |
| `RedPacketService` | 8103 | 红包创建、领取、余额流水、领完处理和过期退款。 |
| `GateWay` | 10010 | HTTP 请求入口、JWT 校验及服务路由。 |

## 核心消息流程

```text
登录
客户端 -> GateWay -> UserService -> MySQL / Redis Token

发送普通消息
客户端 -> WebSocket :9101 -> RealTimeService
         -> Kafka store-topic -> OfflineDataService -> MySQL
         -> Kafka push-topic  -> RealTimeService -> 在线用户 WebSocket

用户离线后重新进入
客户端 -> GateWay -> OfflineDataService
       -> UserService 查询会话 -> Redis 热消息 / MySQL 历史消息
```

### 红包流程

```text
发送红包
RedPacketService 校验权限
  -> 扣减发送者余额、创建红包记录、写余额流水
  -> Redis 写入领取授权、群成员快照和金额池
  -> 投递红包聊天消息到 store-topic / push-topic
  -> 登记 24 小时后的过期任务

领取红包
客户端 -> RedPacketService
  -> Redis Lua 原子校验资格、避免重复领取、弹出一份金额
  -> Kafka 异步写领取记录、增加领取者余额、写余额流水
  -> 最后一份被领取时，红包状态收口为“已领完”
```

## 技术栈

- Java 17
- Spring Boot 3.5.13
- Spring Cloud 2025.0.0
- Spring Cloud Alibaba / Nacos
- MyBatis-Plus、MySQL
- Redis、Kafka、Canal
- Netty WebSocket
- OpenFeign、Resilience4j、ShedLock
- MinIO

## 运行前准备

请先准备以下基础设施，并在各服务的 `application.yml` 中配置连接信息：

- MySQL：创建项目所需数据库和表结构
- Redis
- Kafka
- Nacos
- Canal：用于将消息表变更同步到 Redis 热消息集合
- MinIO：用于头像等对象存储

> 配置文件可能包含本地开发连接信息。请在部署前通过环境变量、配置中心或独立配置文件替换敏感账号和密码，避免提交真实凭据。

## 构建与启动

### 1. 安装公共模块

```powershell
mvn -pl Common install
```

### 2. 按依赖顺序启动服务

建议顺序：

1. 启动 MySQL、Redis、Kafka、Nacos、Canal、MinIO；
2. 启动 `UserService`；
3. 启动 `OfflineDataService`、`RealTimeService`、`RedPacketService`；
4. 最后启动 `GateWay`。

每个服务均可在项目根目录执行：

```powershell
mvn -pl UserService spring-boot:run
mvn -pl OfflineDataService spring-boot:run
mvn -pl RealTimeService spring-boot:run
mvn -pl RedPacketService spring-boot:run
mvn -pl GateWay spring-boot:run
```

也可以构建上述模块：

```powershell
mvn clean package -pl Common,UserService,OfflineDataService,RealTimeService,RedPacketService,GateWay -am
```

## 主要访问入口

| 类型 | 地址 / 路径 | 说明 |
| --- | --- | --- |
| 网关 | `http://localhost:10010` | 用户和离线消息等 HTTP 请求入口。 |
| WebSocket | `ws://localhost:9101/ws/netty` | 实时消息连接；握手时携带 `Authorization` 请求头。 |
| 用户接口 | `/api/user/**` | 注册、登录、令牌刷新、头像、用户会话等。 |
| 联系人接口 | `/api/contact/**` | 搜索用户、好友申请、好友列表、拉黑等。 |
| 群组接口 | `/api/group/**` | 创建群组、邀请群成员。 |
| 消息接口 | `/api/message/**` | 离线消息、历史消息等。 |
| 红包接口 | `/api/chat/redPacket/**` | 发送、领取、查询红包信息。 |

## 注意事项

- HTTP 网关使用 `Access-Token` 和 `Refresh-Token` 请求头；WebSocket 握手使用 `Authorization` 请求头。
- WebSocket 服务端口为 `9101`，与 `RealTimeService` 的 Spring Boot HTTP 端口 `8102` 不同。
- 红包金额以“分”进行计算和存储；领取、余额入账和状态收口通过 Redis 与 Kafka 协作完成。
- 启动前应确认 Kafka Topic、Nacos 注册、Canal 订阅和数据库表结构均已准备完成。
