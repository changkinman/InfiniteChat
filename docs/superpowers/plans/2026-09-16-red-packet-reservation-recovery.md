# 红包领取 Redis 预占恢复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 MySQL 领取事务回滚或应用宕机后，自动收敛 Redis 红包预占，避免库存泄漏和用户永久处于“已领取”状态。

**Architecture:** 使用 `StringRedisTemplate` 维护每次领取的带 UUID token 的 Redis 预占 Hash 和全局超时 ZSet。MySQL `red_packet_receive` 是最终事实；事务完成回调尽快确认或释放，10 秒调度器对超过 60 秒的 `PENDING` 预占按 MySQL 查询结果执行同一套 token 校验 Lua。

**Tech Stack:** Java 8、Spring Boot 2.6、Spring Data Redis Lua、Spring Transaction Synchronization、MyBatis-Plus、JUnit 5、Mockito。

**Spec:** `docs/superpowers/specs/2026-09-16-red-packet-reservation-recovery-design.md`

## Global Constraints

- MySQL `red_packet_receive` 记录是成功领取的唯一最终事实；Redis 不作为资金账本。
- Redis 状态固定为 `PENDING=1`、`CONFIRMED=2`、`RELEASED=3`；token 使用 `UUID.randomUUID().toString()`。
- `PENDING` 超时固定为 60 秒；扫描调度固定延迟为 10 秒；每次最多处理 100 条。
- 确认和释放必须同时匹配红包 ID、用户 ID、`PENDING` 和 reservation token；token 不匹配时不得改变库存。
- 不引入 XA、消息队列、分布式锁或新依赖。
- 数据库唯一索引必须在部署前由运维执行；迁移前若有重复数据必须人工清理。

---

## File structure

- `MessageingService/src/main/java/.../redpacket/RedPacketReservationRegistry.java`：Redis key、Lua 脚本及预占状态转换。
- `MessageingService/src/main/java/.../redpacket/RedPacketReservation.java`：超时扫描和事务回调的不可变预占值。
- `MessageingService/src/main/java/.../redpacket/ReservationResult.java`：`RESERVED`、`PENDING`、`EMPTY`、`ALREADY_CLAIMED` 结果与 token。
- `MessageingService/src/main/java/.../redpacket/RedPacketReservationRecoveryJob.java`：按 MySQL 事实确认或释放超时预占。
- `MessageingService/src/main/java/.../service/RedPacketReceiveService.java`：领取编排、事务完成回调和处理中响应。
- `MessageingService/src/main/java/.../mapper/RedPacketReceiveMapper.java`：按红包和用户确定性读取。
- `MessageingService/src/main/java/.../data/receiveRedPackage/ReceiveRedPacketResponse.java`：处理中用户消息。
- `docs/sql/2026-09-16-red-packet-receive-unique-index.sql`：唯一索引部署 DDL。

### Task 1: Token-guarded Redis reservation registry

**Files:**

- Create: `MessageingService/src/main/java/com/shanyangcode/infinitechat/messageingservice/redpacket/RedPacketReservation.java`
- Create: `MessageingService/src/main/java/com/shanyangcode/infinitechat/messageingservice/redpacket/ReservationResult.java`
- Create: `MessageingService/src/main/java/com/shanyangcode/infinitechat/messageingservice/redpacket/RedPacketReservationRegistry.java`
- Test: `MessageingService/src/test/java/com/shanyangcode/infinitechat/messageingservice/redpacket/RedPacketReservationRegistryTest.java`

**Interfaces:**

- Produces: `ReservationResult reserve(long redPacketId, long userId)`, `boolean confirm(RedPacketReservation reservation)`, `boolean release(RedPacketReservation reservation)`, and `List<RedPacketReservation> findExpiredPending(long cutoffEpochMillis, int limit)`.
- `RedPacketReservation` exposes packet ID, user ID, token and creation epoch.
- `ReservationResult.Status` is `RESERVED`, `PENDING`, `EMPTY` or `ALREADY_CLAIMED`.

- [ ] **Step 1: Write the failing registry tests**

```java
@Test
void reserveReturnsPendingWithoutASecondStockDeduction() {
    when(redis.execute(eq(RedPacketReservationRegistry.RESERVE_SCRIPT), anyList(), any(), any(), any(), any()))
            .thenReturn(Arrays.asList(2L, "existing-token", 100L));

    ReservationResult result = registry.reserve(88L, 1001L);

    assertEquals(ReservationResult.Status.PENDING, result.getStatus());
    verify(redis).execute(eq(RedPacketReservationRegistry.RESERVE_SCRIPT),
            eq(Arrays.asList("red_packet:count:88", "red_packet:reservation:88:1001", "red_packet:pending")),
            any(), eq("1"), eq("1001"), any(), any());
}

@Test
void releaseDoesNotReportSuccessWhenLuaRejectsAnOldToken() {
    RedPacketReservation old = new RedPacketReservation(88L, 1001L, "old-token", 100L);
    when(redis.execute(eq(RedPacketReservationRegistry.RELEASE_SCRIPT), anyList(), any(), any(), any(), any()))
            .thenReturn(0L);

    assertFalse(registry.release(old));
}
```

- [ ] **Step 2: Run the registry test to verify RED**

Run: `mvn -pl MessageingService -Dtest=RedPacketReservationRegistryTest test`

Expected: compilation failure because the registry and value types do not exist.

- [ ] **Step 3: Write the minimal registry implementation**

```java
public ReservationResult reserve(long redPacketId, long userId) {
    String token = UUID.randomUUID().toString();
    long now = clock.millis();
    List<?> reply = redis.execute(RESERVE_SCRIPT, keys(redPacketId, userId),
            token, "1", String.valueOf(userId), String.valueOf(now), pendingMember(redPacketId, userId, token));
    return ReservationResult.fromLua(reply, redPacketId, userId, token, now);
}

public boolean release(RedPacketReservation reservation) {
    Long changed = redis.execute(RELEASE_SCRIPT, keys(reservation), reservation.getToken(),
            "1", "3", pendingMember(reservation));
    return Long.valueOf(1L).equals(changed);
}
```

Use `StringRedisTemplate`. `reserve.lua` checks the reservation Hash before decrementing: return `PENDING` for state `1`, `ALREADY_CLAIMED` for state `2`; otherwise verify count, decrement, write state/token/createdAt, copy the count TTL, and `ZADD` the pending member. `confirm.lua` and `release.lua` reject a non-`PENDING` state or unequal token. They `ZREM` only the exact member; release increments only an extant count key and writes state `3`.

- [ ] **Step 4: Run registry tests to verify GREEN**

Run: `mvn -pl MessageingService -Dtest=RedPacketReservationRegistryTest test`

Expected: PASS; matched-token and old-token paths pass.

- [ ] **Step 5: Commit the registry unit**

Run: `git add MessageingService/src/main/java/com/shanyangcode/infinitechat/messageingservice/redpacket MessageingService/src/test/java/com/shanyangcode/infinitechat/messageingservice/redpacket`

Run: `git commit -m "feat: add token guarded red packet reservations"`

### Task 2: Tie reservations to the receive transaction and database idempotency

**Files:**

- Modify: `MessageingService/src/main/java/com/shanyangcode/infinitechat/messageingservice/service/RedPacketReceiveService.java`
- Modify: `MessageingService/src/main/java/com/shanyangcode/infinitechat/messageingservice/mapper/RedPacketReceiveMapper.java`
- Modify: `MessageingService/src/main/java/com/shanyangcode/infinitechat/messageingservice/data/receiveRedPackage/ReceiveRedPacketResponse.java`
- Create: `docs/sql/2026-09-16-red-packet-receive-unique-index.sql`
- Test: `MessageingService/src/test/java/com/shanyangcode/infinitechat/messageingservice/service/RedPacketReceiveServiceTest.java`

**Interfaces:**

- Consumes: `RedPacketReservationRegistry.reserve/confirm/release`.
- Produces: `status=4` and `message=领取处理中，请稍后重试` for an existing `PENDING`; success and prior-success responses retain existing status and amount.
- `RedPacketReceiveMapper.selectByPacketIdAndReceiverId(Long redPacketId, Long userId)` returns one record or `null`.

- [ ] **Step 1: Write failing service tests**

```java
@Test
void pendingReservationReturnsProcessingInsteadOfClaimed() {
    when(registry.reserve(88L, 1001L)).thenReturn(ReservationResult.pending());

    ReceiveRedPacketResponse response = service.receiveRedPacket(1001L, 88L);

    assertEquals(Integer.valueOf(4), response.getStatus());
    assertEquals("领取处理中，请稍后重试", response.getMessage());
    verifyNoInteractions(redPacketMapper);
}

@Test
void afterCommitConfirmsTheExactReservation() {
    RedPacketReservation reservation = new RedPacketReservation(88L, 1001L, "token-a", 1L);
    when(registry.reserve(88L, 1001L)).thenReturn(ReservationResult.reserved(reservation));
    when(redPacketMapper.selectByIdForUpdate(88L)).thenReturn(unclaimedPacket(88L, 10, new BigDecimal("10.00")));
    when(userBalanceMapper.selectById(1001L)).thenReturn(balance(1001L, "0.00"));
    when(redPacketMapper.updateById(any(RedPacket.class))).thenReturn(1);
    when(redPacketReceiveMapper.insert(any(RedPacketReceive.class))).thenReturn(1);
    when(userBalanceMapper.updateById(any(UserBalance.class))).thenReturn(1);
    when(balanceLogMapper.insert(any(BalanceLog.class))).thenReturn(1);

    service.receiveRedPacket(1001L, 88L);
    invokeRegisteredAfterCommit();

    verify(registry).confirm(reservation);
}
```

- [ ] **Step 2: Run service tests to verify RED**

Run: `mvn -pl MessageingService -Dtest=RedPacketReceiveServiceTest test`

Expected: compilation failure because constructor injection and processing message do not exist.

- [ ] **Step 3: Write the minimal transaction integration**

```java
ReservationResult result = reservationRegistry.reserve(redPacketId, userId);
if (result.getStatus() == ReservationResult.Status.PENDING) {
    return new ReceiveRedPacketResponse(null, 4, "领取处理中，请稍后重试");
}
if (result.getStatus() != ReservationResult.Status.RESERVED) {
    return new ReceiveRedPacketResponse(null, CLAIMED);
}
RedPacketReservation reservation = result.getReservation();
TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
    @Override public void afterCommit() { safeConfirm(reservation); }
    @Override public void afterCompletion(int status) {
        if (status == STATUS_ROLLED_BACK) safeRelease(reservation);
    }
});
```

Replace the current claimed Set Lua calls with the registry. Retain early release for an expired or empty locked packet. Remove catch-time unconditional release because `afterCompletion` owns rollback. `safeConfirm` and `safeRelease` log Redis exceptions without changing the committed MySQL result. Add the mapper query. Add a three-argument response constructor while retaining the existing two-argument constructor. Add the exact DDL:

```sql
ALTER TABLE red_packet_receive
  ADD CONSTRAINT uk_red_packet_receive_packet_receiver
  UNIQUE (red_packet_id, receiver_id);
```

- [ ] **Step 4: Run service tests to verify GREEN**

Run: `mvn -pl MessageingService -Dtest=RedPacketReceiveServiceTest test`

Expected: PASS; pending response, commit confirmation, rollback release and existing-record idempotency pass.

- [ ] **Step 5: Commit transaction integration**

Run: `git add MessageingService/src/main/java/com/shanyangcode/infinitechat/messageingservice/service/RedPacketReceiveService.java MessageingService/src/main/java/com/shanyangcode/infinitechat/messageingservice/mapper/RedPacketReceiveMapper.java MessageingService/src/main/java/com/shanyangcode/infinitechat/messageingservice/data/receiveRedPackage/ReceiveRedPacketResponse.java MessageingService/src/test/java/com/shanyangcode/infinitechat/messageingservice/service/RedPacketReceiveServiceTest.java docs/sql/2026-09-16-red-packet-receive-unique-index.sql`

Run: `git commit -m "feat: recover red packet reservations around transactions"`

### Task 3: Recover orphaned pending reservations on a schedule

**Files:**

- Create: `MessageingService/src/main/java/com/shanyangcode/infinitechat/messageingservice/redpacket/RedPacketReservationRecoveryJob.java`
- Modify: `MessageingService/src/main/java/com/shanyangcode/infinitechat/messageingservice/MessageingServiceApplication.java`
- Test: `MessageingService/src/test/java/com/shanyangcode/infinitechat/messageingservice/redpacket/RedPacketReservationRecoveryJobTest.java`

**Interfaces:**

- Consumes: registry `findExpiredPending/confirm/release` and mapper `selectByPacketIdAndReceiverId`.
- Produces: `recoverExpiredReservations()`, annotated `@Scheduled(fixedDelay = 10000)`, processing at most 100 records older than `System.currentTimeMillis() - 60000`.

- [ ] **Step 1: Write failing recovery-job tests**

```java
@Test
void persistedReceiveConfirmsExpiredPendingReservation() {
    RedPacketReservation reservation = new RedPacketReservation(88L, 1001L, "token-a", 1L);
    when(registry.findExpiredPending(anyLong(), eq(100))).thenReturn(Collections.singletonList(reservation));
    when(mapper.selectByPacketIdAndReceiverId(88L, 1001L)).thenReturn(new RedPacketReceive());

    job.recoverExpiredReservations();

    verify(registry).confirm(reservation);
    verify(registry, never()).release(reservation);
}

@Test
void missingReceiveReleasesExpiredPendingReservation() {
    RedPacketReservation reservation = new RedPacketReservation(88L, 1001L, "token-a", 1L);
    when(registry.findExpiredPending(anyLong(), eq(100))).thenReturn(Collections.singletonList(reservation));

    job.recoverExpiredReservations();

    verify(registry).release(reservation);
}
```

- [ ] **Step 2: Run recovery-job test to verify RED**

Run: `mvn -pl MessageingService -Dtest=RedPacketReservationRecoveryJobTest test`

Expected: compilation failure because the recovery job does not exist.

- [ ] **Step 3: Write bounded recovery implementation and enable scheduling**

```java
@Scheduled(fixedDelay = 10_000L)
public void recoverExpiredReservations() {
    long cutoff = System.currentTimeMillis() - 60_000L;
    for (RedPacketReservation reservation : registry.findExpiredPending(cutoff, 100)) {
        if (mapper.selectByPacketIdAndReceiverId(reservation.getRedPacketId(), reservation.getUserId()) != null) {
            registry.confirm(reservation);
        } else {
            registry.release(reservation);
        }
    }
}
```

Wrap each reservation in `try/catch`, log packet and user IDs, and continue the batch. Add `@EnableScheduling` to `MessageingServiceApplication`; duplicate multi-node scans are harmless because Lua token matching permits only one state transition.

- [ ] **Step 4: Run recovery-job tests to verify GREEN**

Run: `mvn -pl MessageingService -Dtest=RedPacketReservationRecoveryJobTest test`

Expected: PASS for the MySQL-present confirmation and MySQL-absent release paths.

- [ ] **Step 5: Run module verification and commit**

Run: `mvn -pl MessageingService test`

Expected: all MessageingService tests pass.

Run: `git add MessageingService/src/main/java/com/shanyangcode/infinitechat/messageingservice/redpacket/RedPacketReservationRecoveryJob.java MessageingService/src/main/java/com/shanyangcode/infinitechat/messageingservice/MessageingServiceApplication.java MessageingService/src/test/java/com/shanyangcode/infinitechat/messageingservice/redpacket/RedPacketReservationRecoveryJobTest.java`

Run: `git commit -m "feat: reconcile orphaned red packet reservations"`

## Plan self-review

- Spec coverage: Tasks 1–3 respectively cover tokenized state/Lua, transaction plus database idempotency, and the 10-second / 60-second recovery scan. The unique index deployment instruction is in Task 2.
- Placeholder scan: no TBD/TODO placeholders; each task gives commands, interfaces, and required behavior.
- Type consistency: `RedPacketReservation`, `ReservationResult`, `RedPacketReservationRegistry`, and the mapper query use the same names in all tasks.
