# 红包领取 Redis 预占恢复设计

## 1. 目标与范围

领取红包目前先在 Redis 原子扣减库存并标记用户已领取，再在本地 MySQL 事务中写入红包、余额与领取记录。进程在 Redis 预占后宕机时，MySQL 最终回滚而 Redis 预占无法由 JVM 的异常处理归还，导致库存泄漏和用户被错误地视为已领取。

本次实现一个最小恢复机制：Redis 将预占表示为带唯一 token 的状态机；定时任务扫描超时预占，并以 MySQL `red_packet_receive` 的成功记录为最终事实确认或释放该预占。

本次包括：

- `PENDING`、`CONFIRMED`、`RELEASED` 状态及带 token 的 Redis Lua 预占、确认和释放。
- 超过 60 秒的 `PENDING` 每 10 秒扫描一次并补偿。
- 本机事务提交后确认、回滚后释放；这些即时动作失败或 JVM 宕机均由扫描器收敛。
- `red_packet_receive(red_packet_id, receiver_id)` 唯一索引，作为 MySQL 幂等防线。
- 同用户已有未超时 `PENDING` 时返回“领取处理中，请稍后重试”。

本次不包括：XA/两阶段提交、消息队列、持久化补偿任务表、告警系统、分布式定时锁、复杂人工修复工具。

## 2. 一致性原则

- MySQL 中已提交的 `red_packet_receive` 记录是领取成功的最终事实；Redis 是高并发库存闸门和可重建状态，不是资金账本。
- 每次 Redis 预占生成随机 `reservationToken`。确认和释放 Lua 必须同时匹配红包、用户、`PENDING` 状态和 token，旧任务不能影响同一用户的后续预占。
- 60 秒须大于正常领取事务的最长执行时间。扫描器只处理已超时记录；正常事务仍在该窗口内完成。
- 所有释放操作均幂等：已确认、已释放、token 不匹配或记录不存在时均不回补库存。

## 3. Redis 数据模型

```text
red_packet:count:{redPacketId}                 String，剩余份数（现有）
red_packet:reservation:{redPacketId}:{userId} Hash
  state       PENDING | CONFIRMED | RELEASED
  token       UUID
  createdAt   epoch milliseconds

red_packet:pending                              ZSet
  score       PENDING 创建时间（epoch milliseconds）
  member      {redPacketId}:{userId}:{token}
```

预占 Hash 的 TTL 与红包库存 key 对齐；`CONFIRMED` 和 `RELEASED` 也沿用该 TTL。扫描器从 `red_packet:pending` 以 `now - 60s` 查询一批到期 member，因此能够取得原始 token，而不是仅凭红包和用户猜测要补偿哪一次预占。

## 4. 领取与恢复流程

```text
客户端
  -> 预查 MySQL 是否已有领取记录：有则直接返回已领取金额
  -> reserve.lua：库存 -1，写 PENDING(token)，ZADD pending
  -> MySQL 本地事务：锁红包、更新余额与红包、插入领取记录
      -> commit：confirm.lua(token)，状态改 CONFIRMED，ZREM pending
      -> rollback：release.lua(token)，状态改 RELEASED，库存 +1，ZREM pending

扫描器（每 10 秒）
  -> 取创建时间 <= now - 60s 的 pending member（含 token）
  -> 查询 MySQL red_packet_receive(redPacketId, userId)
      -> 已存在：confirm.lua(token)
      -> 不存在：release.lua(token)
```

`reserve.lua` 发现相同用户的有效 `PENDING` 时不扣库存，返回“处理中”；发现 `CONFIRMED` 时由入口的 MySQL 查询返回既有金额。扫描器可在多实例并发执行，因为只有 token 匹配的 Lua 调用能改变状态和库存。

应用在 MySQL 提交后、确认 Redis 前宕机时，扫描器会发现 MySQL 成功记录并确认；应用在 Redis 预占后、MySQL 回滚前或回滚后宕机时，扫描器会在超时且无成功记录后释放。普通事务回滚通过 `afterCompletion(ROLLED_BACK)` 尽快释放，但不能替代扫描器。

## 5. 数据库与接口约束

部署前执行：

```sql
ALTER TABLE red_packet_receive
  ADD CONSTRAINT uk_red_packet_receive_packet_receiver
  UNIQUE (red_packet_id, receiver_id);
```

若历史数据已存在重复 `(red_packet_id, receiver_id)`，迁移必须先停止并清理重复数据，不能静默创建索引。插入领取记录遇到唯一键冲突时，事务回滚，后续查询返回已持久化的领取金额。

现有领取成功响应保持不变。新增的处理中结果复用现有业务错误/响应约定并明确提示“领取处理中，请稍后重试”；不把它伪装为“已领完”。

## 6. 实现边界和验证

预期修改范围：红包常量和 Lua 脚本封装、`RedPacketReceiveService` 的事务完成回调、恢复调度器、应用的 `@EnableScheduling`、领取记录 mapper 查询、数据库迁移说明和测试。

至少覆盖：

1. 预占、确认、释放各自的 token 条件与幂等性。
2. 已存在 `PENDING` 时不重复扣库存且返回处理中。
3. 回滚后立即释放；提交后确认。
4. 扫描器对“DB 有领取记录”和“DB 无领取记录”分别确认和释放。
5. 旧 token 的延迟补偿不能释放新 token 的预占。
6. 同一红包、同一用户的重复领取由数据库唯一约束拒绝。

Redis 过期通知仅继续承担红包到期退款，不能作为预占补偿的唯一触发来源；服务停机期间的 keyspace notification 可能丢失。
