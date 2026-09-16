# Consistent Hash Routing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Route each WebSocket handshake to a stable Nacos `NettyService` instance with consistent hashing, then maintain the connection's authoritative RTC endpoint and ownership in Redis for precise post-connect delivery.

**Architecture:** Gateway validates `userUuid` and uses a `NettyService`-scoped Spring Cloud LoadBalancer backed by an immutable virtual-node hash ring. RTC claims a versioned connection route in Redis, tracks the matching local Channel, renews ownership on heartbeat, conditionally releases it on disconnect, and asks the previous node to close an exact stale connection. Messaging and Contact services read the Redis Hash `endpoint` field for directed pushes.

**Tech Stack:** Java 8, Spring Boot 2.6.13, Spring Cloud Gateway 3.1.3, Spring Cloud LoadBalancer 3.1.4, Nacos Discovery 2021.0.5.0, Spring Data Redis, Netty, JUnit 5, Mockito, Maven.

**Spec:** `docs/superpowers/specs/2026-09-15-consistent-hash-routing-design.md`

## Global Constraints

- Consistent hashing is used only for WebSocket connection establishment; Redis remains authoritative after connection establishment.
- Hash key: the non-blank `userUuid` WebSocket handshake header; Gateway returns HTTP 400 when it is absent or blank.
- Node source: Nacos `NettyService`; no Redis-based node discovery.
- Nodes are equal weight; default virtual-node count is exactly `128` and configurable.
- Hash implementation uses JDK SHA-256; add no hashing library.
- One active connection per user under normal operation; cross-node takeover uses internal HTTP and is eventually consistent when the old node is unreachable.
- Redis route key: `user:session:{userId}` as a Hash with `nodeId`, `endpoint`, and `connectionId`; default TTL is exactly `10m` and configurable.
- Existing String-valued online routes require a maintenance-window cleanup before deployment; do not implement mixed-version rolling compatibility.
- Do not optimize the existing group-chat or Moment broadcast paths in this change.
- Do not add Maven dependencies. Run Maven with `-o` first; if an artifact is missing, stop and obtain user permission before allowing any download into a non-virtual environment or global Maven repository.
- Preserve the pre-existing untracked `.claude/` directory and unrelated user changes.

---

## File Structure

### Gateway

- Create `GateWay/src/main/java/com/shanyangcode/infinitechat/gateway/routing/ConsistentHashProperties.java`: validated virtual-node configuration.
- Create `GateWay/src/main/java/com/shanyangcode/infinitechat/gateway/routing/ConsistentHashRing.java`: immutable SHA-256 virtual-node ring.
- Create `GateWay/src/main/java/com/shanyangcode/infinitechat/gateway/routing/ConsistentHashLoadBalancer.java`: request-aware LoadBalancer and atomic ring snapshot cache.
- Create `GateWay/src/main/java/com/shanyangcode/infinitechat/gateway/routing/NettyLoadBalancerConfiguration.java`: binds the custom LoadBalancer for `NettyService`.
- Create `GateWay/src/main/java/com/shanyangcode/infinitechat/gateway/routing/WebSocketUserIdFilter.java`: returns 400 for an invalid WebSocket routing key.
- Modify `GateWay/src/main/java/com/shanyangcode/infinitechat/gateway/GateWayApplication.java`: attach the `NettyService`-specific LoadBalancer configuration.
- Modify `GateWay/src/main/resources/application.yaml`: add `infinitechat.routing.consistent-hash.virtual-nodes`.
- Create focused tests under `GateWay/src/test/java/com/shanyangcode/infinitechat/gateway/routing/`.

### RTC

- Create `RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/routing/OnlineRoute.java`: immutable route value.
- Create `RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/routing/OnlineRouteProperties.java`: TTL and advertised endpoint configuration.
- Create `RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/routing/ConnectionTakeoverProperties.java`: bounded internal HTTP timeouts.
- Create `RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/routing/OnlineRouteRegistry.java`: all Redis route reads and scripts.
- Create Redis scripts under `RealTimeCommunicationService/src/main/resources/scripts/online-route/`.
- Create `RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/websocket/ConnectionBinding.java`: user, connection ID, and Channel tuple.
- Modify `RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/websocket/ChannelManager.java`: ownership-aware singleton component.
- Create `RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/routing/LocalNode.java`: immutable local node identity and advertised endpoint.
- Create `RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/routing/RoutingConfiguration.java`: derives `LocalNode` and the dedicated takeover HTTP client.
- Create `RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/routing/ConnectionTakeoverClient.java`: bounded-time internal HTTP client.
- Create `RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/routing/ConnectionLifecycleService.java`: connection claim, recheck, renewal, release, and takeover orchestration.
- Create `RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/data/connection/ConnectionTakeoverRequest.java` and `controller/InternalConnectionController.java`: exact-connection kick API.
- Modify `MessageInboundHandler.java`, `NettyServer.java`, `NettyMessageService.java`, and `PerfMetricsController.java` to use injected connection components.
- Modify `RealTimeCommunicationService/src/main/resources/application.yml` and `application-perf.yml` with routing configuration.
- Create focused tests under the matching RTC test packages.

### Route consumers and documentation

- Create an `OnlineRouteLookup` in both Messaging and Contact services; each reads the Redis Hash `endpoint` field.
- Modify `MessageingService/.../MessageServiceImpl.java` and `ContanctService/.../PushServiceImpl.java` to use the endpoint directly.
- Create `docs/consistent-hash-routing-runbook.md`: maintenance migration and multi-node manual verification.

---

### Task 1: Immutable Consistent Hash Ring

**Files:**
- Create: `GateWay/src/main/java/com/shanyangcode/infinitechat/gateway/routing/ConsistentHashProperties.java`
- Create: `GateWay/src/main/java/com/shanyangcode/infinitechat/gateway/routing/ConsistentHashRing.java`
- Test: `GateWay/src/test/java/com/shanyangcode/infinitechat/gateway/routing/ConsistentHashRingTest.java`

**Interfaces:**
- Consumes: `List<ServiceInstance>` where physical identity is `instance.getHost() + ":" + instance.getPort()`.
- Produces: `ConsistentHashRing(List<ServiceInstance> instances, int virtualNodes)`, `ServiceInstance select(String key)`, and `List<String> nodeIds()`.
- Produces: `ConsistentHashProperties#getVirtualNodes()` bound to `infinitechat.routing.consistent-hash.virtual-nodes` with default `128`.

- [ ] **Step 1: Write the failing ring tests**

```java
class ConsistentHashRingTest {
    private final ServiceInstance n1 = node("n1", "10.0.0.1", 9000);
    private final ServiceInstance n2 = node("n2", "10.0.0.2", 9000);
    private final ServiceInstance n3 = node("n3", "10.0.0.3", 9000);

    @Test
    void sameUserAndReorderedInstancesChooseSameNode() {
        ConsistentHashRing first = new ConsistentHashRing(Arrays.asList(n1, n2, n3), 128);
        ConsistentHashRing reordered = new ConsistentHashRing(Arrays.asList(n3, n1, n2), 128);
        assertEquals(id(first.select("10001")), id(reordered.select("10001")));
    }

    @Test
    void addingNodeMovesOnlyPartOfUsers() {
        ConsistentHashRing before = new ConsistentHashRing(Arrays.asList(n1, n2, n3), 128);
        ConsistentHashRing after = new ConsistentHashRing(Arrays.asList(n1, n2, n3, node("n4", "10.0.0.4", 9000)), 128);
        long moved = LongStream.range(0, 10_000)
                .filter(i -> !id(before.select(Long.toString(i))).equals(id(after.select(Long.toString(i)))))
                .count();
        assertTrue(moved > 1_500 && moved < 3_500, "moved=" + moved);
    }

    @Test
    void removingNodeMovesOnlyUsersOwnedByThatNode() {
        ServiceInstance n4 = node("n4", "10.0.0.4", 9000);
        ConsistentHashRing before = new ConsistentHashRing(Arrays.asList(n1, n2, n3, n4), 128);
        ConsistentHashRing after = new ConsistentHashRing(Arrays.asList(n1, n2, n3), 128);
        LongStream.range(0, 10_000).forEach(i -> {
            String oldNode = id(before.select(Long.toString(i)));
            String newNode = id(after.select(Long.toString(i)));
            if (!oldNode.equals(newNode)) assertEquals(id(n4), oldNode);
        });
    }

    @Test
    void virtualNodesAvoidSevereSkew() {
        ConsistentHashRing ring = new ConsistentHashRing(Arrays.asList(n1, n2, n3), 128);
        Map<String, Long> counts = LongStream.range(0, 10_000).boxed()
                .collect(Collectors.groupingBy(i -> id(ring.select(i.toString())), Collectors.counting()));
        assertEquals(3, counts.size());
        assertTrue(Collections.max(counts.values()) - Collections.min(counts.values()) < 1_500, counts.toString());
    }
}
```

Use `DefaultServiceInstance` in the `node` helper and return `host:port` from `id`.

- [ ] **Step 2: Run the test and verify the red state**

Run:

```powershell
mvn -o -f GateWay/pom.xml -Dtest=ConsistentHashRingTest test
```

Expected: compilation fails because `ConsistentHashRing` does not exist.

- [ ] **Step 3: Implement the minimal immutable ring**

```java
public final class ConsistentHashRing {
    private final NavigableMap<BigInteger, ServiceInstance> ring;
    private final List<String> nodeIds;

    public ConsistentHashRing(List<ServiceInstance> instances, int virtualNodes) {
        if (virtualNodes <= 0) throw new IllegalArgumentException("virtualNodes must be greater than zero");
        List<ServiceInstance> sorted = new ArrayList<>(instances);
        sorted.sort(Comparator.comparing(ConsistentHashRing::nodeId));
        NavigableMap<BigInteger, ServiceInstance> positions = new TreeMap<>();
        for (ServiceInstance instance : sorted) {
            for (int replica = 0; replica < virtualNodes; replica++) {
                positions.put(hash(nodeId(instance) + "#" + replica), instance);
            }
        }
        this.ring = Collections.unmodifiableNavigableMap(positions);
        this.nodeIds = Collections.unmodifiableList(sorted.stream()
                .map(ConsistentHashRing::nodeId).collect(Collectors.toList()));
    }

    public ServiceInstance select(String key) {
        if (ring.isEmpty()) return null;
        Map.Entry<BigInteger, ServiceInstance> entry = ring.ceilingEntry(hash(key));
        return (entry != null ? entry : ring.firstEntry()).getValue();
    }

    private static BigInteger hash(String value) {
        try {
            return new BigInteger(1, MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
```

Add a validated properties class without a new dependency:

```java
@Component
@ConfigurationProperties(prefix = "infinitechat.routing.consistent-hash")
public final class ConsistentHashProperties {
    private int virtualNodes = 128;
    public int getVirtualNodes() { return virtualNodes; }
    public void setVirtualNodes(int virtualNodes) { this.virtualNodes = virtualNodes; }
    @PostConstruct
    void validate() {
        if (virtualNodes <= 0) throw new IllegalStateException("virtual-nodes must be greater than zero");
    }
}
```

- [ ] **Step 4: Run the ring tests and verify green**

```powershell
mvn -o -f GateWay/pom.xml -Dtest=ConsistentHashRingTest test
```

Expected: all `ConsistentHashRingTest` tests pass with zero failures.

- [ ] **Step 5: Commit the ring**

```powershell
git add GateWay/src/main/java/com/shanyangcode/infinitechat/gateway/routing/ConsistentHashProperties.java GateWay/src/main/java/com/shanyangcode/infinitechat/gateway/routing/ConsistentHashRing.java GateWay/src/test/java/com/shanyangcode/infinitechat/gateway/routing/ConsistentHashRingTest.java
git commit -m "feat(gateway): add consistent hash ring"
```

---

### Task 2: Gateway WebSocket Validation and Netty LoadBalancer

**Files:**
- Create: `GateWay/src/main/java/com/shanyangcode/infinitechat/gateway/routing/ConsistentHashLoadBalancer.java`
- Create: `GateWay/src/main/java/com/shanyangcode/infinitechat/gateway/routing/NettyLoadBalancerConfiguration.java`
- Create: `GateWay/src/main/java/com/shanyangcode/infinitechat/gateway/routing/WebSocketUserIdFilter.java`
- Modify: `GateWay/src/main/java/com/shanyangcode/infinitechat/gateway/GateWayApplication.java`
- Modify: `GateWay/src/main/resources/application.yaml`
- Test: `GateWay/src/test/java/com/shanyangcode/infinitechat/gateway/routing/ConsistentHashLoadBalancerTest.java`
- Test: `GateWay/src/test/java/com/shanyangcode/infinitechat/gateway/routing/WebSocketUserIdFilterTest.java`

**Interfaces:**
- Consumes: Task 1 `ConsistentHashRing` and `ConsistentHashProperties`.
- Produces: `ConsistentHashLoadBalancer(ObjectProvider<ServiceInstanceListSupplier>, int)` implementing `ReactorServiceInstanceLoadBalancer`.
- Produces: `WebSocketUserIdFilter` implementing reactive `WebFilter` for exact path `/api/v1/netty`.
- Produces: `@LoadBalancerClient(name = "NettyService", configuration = NettyLoadBalancerConfiguration.class)` on the Gateway application.

- [ ] **Step 1: Write failing LoadBalancer and filter tests**

```java
@Test
void choosesByUserUuidAndIgnoresSupplierOrder() {
    ConsistentHashLoadBalancer balancer = balancerWith(n1, n2, n3);
    Response<ServiceInstance> first = balancer.choose(request("10001")).block();
    Response<ServiceInstance> second = balancerWith(n3, n1, n2).choose(request("10001")).block();
    assertTrue(first.hasServer());
    assertEquals(first.getServer().getHost(), second.getServer().getHost());
}

@Test
void returnsEmptyResponseWhenNacosHasNoInstances() {
    Response<ServiceInstance> response = balancerWith().choose(request("10001")).block();
    assertFalse(response.hasServer());
}

@Test
void rebuildsSnapshotOnlyWhenMembershipChanges() {
    MutableSupplier supplier = new MutableSupplier(Arrays.asList(n1, n2));
    ConsistentHashLoadBalancer balancer = balancerWith(supplier);
    balancer.choose(request("10001")).block();
    ConsistentHashRing first = balancer.ringSnapshot();
    supplier.set(Arrays.asList(n2, n1));
    balancer.choose(request("10001")).block();
    assertSame(first, balancer.ringSnapshot());
    supplier.set(Arrays.asList(n1, n2, n3));
    balancer.choose(request("10001")).block();
    assertNotSame(first, balancer.ringSnapshot());
}

@Test
void missingUserUuidOnWebSocketPathReturnsBadRequest() {
    MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/netty"));
    AtomicBoolean continued = new AtomicBoolean();
    new WebSocketUserIdFilter().filter(exchange, e -> { continued.set(true); return Mono.empty(); }).block();
    assertEquals(HttpStatus.BAD_REQUEST, exchange.getResponse().getStatusCode());
    assertFalse(continued.get());
}

@Test
void blankUserUuidOnWebSocketPathReturnsBadRequest() {
    MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/netty").header("userUuid", "   "));
    new WebSocketUserIdFilter().filter(exchange, e -> Mono.empty()).block();
    assertEquals(HttpStatus.BAD_REQUEST, exchange.getResponse().getStatusCode());
}

@Test
void nonWebSocketPathIsUnaffected() {
    MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/chat/messages"));
    AtomicBoolean continued = new AtomicBoolean();
    new WebSocketUserIdFilter().filter(exchange, e -> { continued.set(true); return Mono.empty(); }).block();
    assertTrue(continued.get());
}
```

The test helper must construct a `RequestDataContext` whose `RequestData` contains the `userUuid` header and a deterministic `ServiceInstanceListSupplier` that emits one list. Keep `ringSnapshot()` package-private so the membership-cache test can inspect snapshot identity without reflection.

- [ ] **Step 2: Run the tests and verify they fail**

```powershell
mvn -o -f GateWay/pom.xml -Dtest=ConsistentHashLoadBalancerTest,WebSocketUserIdFilterTest test
```

Expected: compilation fails because the LoadBalancer and filter classes do not exist.

- [ ] **Step 3: Implement request validation and ring snapshot selection**

```java
public final class ConsistentHashLoadBalancer implements ReactorServiceInstanceLoadBalancer {
    private final ObjectProvider<ServiceInstanceListSupplier> supplier;
    private final int virtualNodes;
    private final AtomicReference<ConsistentHashRing> snapshot = new AtomicReference<>();

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        String userUuid = userUuidFrom((RequestDataContext) request.getContext());
        ServiceInstanceListSupplier available = supplier.getIfAvailable();
        if (available == null) return Mono.just(new EmptyResponse());
        return available.get(request).next().map(instances -> {
            if (instances.isEmpty()) return new EmptyResponse();
            ConsistentHashRing ring = ringFor(instances);
            return new DefaultResponse(ring.select(userUuid));
        });
    }
}
```

`ringFor` compares the sorted `host:port` list from `ring.nodeIds()` with the current snapshot and replaces the `AtomicReference` only when membership changes. Do not use Nacos return order in the signature.

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class WebSocketUserIdFilter implements WebFilter {
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!"/api/v1/netty".equals(exchange.getRequest().getPath().value())) return chain.filter(exchange);
        String userUuid = exchange.getRequest().getHeaders().getFirst("userUuid");
        if (!StringUtils.hasText(userUuid)) {
            exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }
}
```

Bind the custom configuration only to `NettyService`; construct the supplier with `LoadBalancerClientFactory#getLazyProvider(serviceName, ServiceInstanceListSupplier.class)`. Add:

```yaml
infinitechat:
  routing:
    consistent-hash:
      virtual-nodes: 128
```

- [ ] **Step 4: Run Gateway focused and module tests**

```powershell
mvn -o -f GateWay/pom.xml -Dtest=ConsistentHashRingTest,ConsistentHashLoadBalancerTest,WebSocketUserIdFilterTest test
mvn -o -f GateWay/pom.xml test
```

Expected: focused tests and the Gateway module test suite pass with zero failures.

- [ ] **Step 5: Commit Gateway routing**

```powershell
git add GateWay/src/main/java/com/shanyangcode/infinitechat/gateway/GateWayApplication.java GateWay/src/main/java/com/shanyangcode/infinitechat/gateway/routing/ConsistentHashLoadBalancer.java GateWay/src/main/java/com/shanyangcode/infinitechat/gateway/routing/NettyLoadBalancerConfiguration.java GateWay/src/main/java/com/shanyangcode/infinitechat/gateway/routing/WebSocketUserIdFilter.java GateWay/src/main/resources/application.yaml GateWay/src/test/java/com/shanyangcode/infinitechat/gateway/routing/ConsistentHashLoadBalancerTest.java GateWay/src/test/java/com/shanyangcode/infinitechat/gateway/routing/WebSocketUserIdFilterTest.java
git commit -m "feat(gateway): route websocket connections consistently"
```

---

### Task 3: Atomic Redis Online Route Registry

**Files:**
- Create: `RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/routing/OnlineRoute.java`
- Create: `RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/routing/OnlineRouteProperties.java`
- Create: `RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/routing/OnlineRouteRegistry.java`
- Create: `RealTimeCommunicationService/src/main/resources/scripts/online-route/claim.lua`
- Create: `RealTimeCommunicationService/src/main/resources/scripts/online-route/renew.lua`
- Create: `RealTimeCommunicationService/src/main/resources/scripts/online-route/release.lua`
- Test: `RealTimeCommunicationService/src/test/java/com/shanyangcode/infinitechat/realtimecommunicationservice/routing/OnlineRouteRegistryTest.java`

**Interfaces:**
- Produces: `OnlineRoute(String nodeId, String endpoint, String connectionId)` with getters and value equality.
- Produces: `Optional<OnlineRoute> claim(String userId, OnlineRoute route)`, `boolean isOwner(String userId, String connectionId)`, `boolean renew(String userId, String connectionId)`, `boolean release(String userId, String connectionId)`, and `Optional<String> findEndpoint(String userId)`.
- Produces: `OnlineRouteProperties` bound to `infinitechat.routing.online-route` with `Duration ttl = Duration.ofMinutes(10)` and optional `advertisedHttpEndpoint`.

- [ ] **Step 1: Write failing registry behavior tests**

```java
@ExtendWith(MockitoExtension.class)
class OnlineRouteRegistryTest {
    @Mock StringRedisTemplate redis;

    @Test
    void claimReturnsPreviousRoute() {
        when(redis.execute(any(RedisScript.class), anyList(), any(), any(), any(), any()))
                .thenReturn(Arrays.asList("10.0.0.1:9000", "http://10.0.0.1:8083", "old-connection"));
        Optional<OnlineRoute> old = registry().claim("42",
                new OnlineRoute("10.0.0.2:9000", "http://10.0.0.2:8083", "new-connection"));
        assertEquals("old-connection", old.get().getConnectionId());
    }

    @Test
    void mismatchedConnectionCannotRenewOrRelease() {
        when(redis.execute(any(RedisScript.class), anyList(), any())).thenReturn(0L);
        assertFalse(registry().renew("42", "stale"));
        assertFalse(registry().release("42", "stale"));
    }

    @Test
    void ownerCheckReadsOnlyConnectionIdField() {
        when(redis.<String, String>opsForHash().get("user:session:42", "connectionId"))
                .thenReturn("current");
        assertTrue(registry().isOwner("42", "current"));
        assertFalse(registry().isOwner("42", "old"));
    }
}
```

Verify the claim arguments explicitly:

```java
verify(redis).execute(eq(OnlineRouteRegistry.CLAIM_SCRIPT),
        eq(Collections.singletonList("user:session:42")),
        eq("10.0.0.2:9000"), eq("http://10.0.0.2:8083"),
        eq("new-connection"), eq("600000"));
```

Keep the three loaded script constants package-private for focused tests; application code still accesses them only through the registry.

- [ ] **Step 2: Run the test and verify red**

```powershell
mvn -o -f RealTimeCommunicationService/pom.xml -Dtest=OnlineRouteRegistryTest test
```

Expected: compilation fails because the route types do not exist.

- [ ] **Step 3: Add the three exact Lua scripts**

`claim.lua`:

```lua
local oldNodeId = redis.call('HGET', KEYS[1], 'nodeId') or ''
local oldEndpoint = redis.call('HGET', KEYS[1], 'endpoint') or ''
local oldConnectionId = redis.call('HGET', KEYS[1], 'connectionId') or ''
redis.call('HSET', KEYS[1],
  'nodeId', ARGV[1],
  'endpoint', ARGV[2],
  'connectionId', ARGV[3])
redis.call('PEXPIRE', KEYS[1], ARGV[4])
return {oldNodeId, oldEndpoint, oldConnectionId}
```

`renew.lua`:

```lua
if redis.call('HGET', KEYS[1], 'connectionId') == ARGV[1] then
  return redis.call('PEXPIRE', KEYS[1], ARGV[2])
end
return 0
```

`release.lua`:

```lua
if redis.call('HGET', KEYS[1], 'connectionId') == ARGV[1] then
  return redis.call('DEL', KEYS[1])
end
return 0
```

- [ ] **Step 4: Implement the registry and properties**

```java
@Component
public final class OnlineRouteRegistry {
    static final String KEY_PREFIX = "user:session:";
    private final StringRedisTemplate redis;
    private final long ttlMillis;

    public Optional<OnlineRoute> claim(String userId, OnlineRoute route) {
        List<?> old = redis.execute(CLAIM, Collections.singletonList(key(userId)),
                route.getNodeId(), route.getEndpoint(), route.getConnectionId(), Long.toString(ttlMillis));
        return decodeOldRoute(old);
    }

    public boolean renew(String userId, String connectionId) {
        Long result = redis.execute(RENEW, Collections.singletonList(key(userId)),
                connectionId, Long.toString(ttlMillis));
        return Long.valueOf(1L).equals(result);
    }

    public boolean release(String userId, String connectionId) {
        Long result = redis.execute(RELEASE, Collections.singletonList(key(userId)), connectionId);
        return Long.valueOf(1L).equals(result);
    }
}
```

Annotate `OnlineRouteProperties` with `@Component` and `@ConfigurationProperties(prefix = "infinitechat.routing.online-route")`; use mutable setters for binding, default TTL to `Duration.ofMinutes(10)`, and validate that TTL is positive in `@PostConstruct`. Load scripts through `DefaultRedisScript` classpath resources. Treat a three-empty-string claim result as no old route; reject blank user IDs and incomplete new routes with `IllegalArgumentException`.

- [ ] **Step 5: Run focused RTC tests and commit**

```powershell
mvn -o -f RealTimeCommunicationService/pom.xml -Dtest=OnlineRouteRegistryTest test
git add RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/routing/OnlineRoute.java RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/routing/OnlineRouteProperties.java RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/routing/OnlineRouteRegistry.java RealTimeCommunicationService/src/main/resources/scripts/online-route RealTimeCommunicationService/src/test/java/com/shanyangcode/infinitechat/realtimecommunicationservice/routing/OnlineRouteRegistryTest.java
git commit -m "feat(rtc): add atomic online route registry"
```

Expected: `OnlineRouteRegistryTest` passes with zero failures before committing.

---

### Task 4: Ownership-Aware Local Channel Manager

**Files:**
- Create: `RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/websocket/ConnectionBinding.java`
- Modify: `RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/websocket/ChannelManager.java`
- Modify: `RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/websocket/MessageInboundHandler.java:29-181`
- Modify: `RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/websocket/NettyServer.java:30-90`
- Modify: `RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/service/impl/NettyMessageService.java`
- Modify: `RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/perf/PerfMetricsController.java`
- Test: `RealTimeCommunicationService/src/test/java/com/shanyangcode/infinitechat/realtimecommunicationservice/websocket/ChannelManagerTest.java`

**Interfaces:**
- Consumes: Netty `Channel`.
- Produces: `ConnectionBinding(String userId, String connectionId, Channel channel)`.
- Produces: instance methods `ConnectionBinding register(ConnectionBinding)`, `Optional<ConnectionBinding> findByUserId(String)`, `Optional<ConnectionBinding> findByChannel(Channel)`, `boolean remove(ConnectionBinding)`, `boolean close(ConnectionBinding)`, `boolean closeIfMatches(String userId, String connectionId)`, `int activeUserCount()`, and `int activeChannelCount()`.
- Preserves for push callers: `Channel getChannelByUserId(String userId)` as an instance method.
- Temporarily preserves instance-form legacy methods used by the current handler: `addUserChannel`, `addChannelUser`, `removeUserChannel`, `removeChannelUser`, and `getUserByChannel`; Task 6 removes them after lifecycle delegation is complete.

- [ ] **Step 1: Write failing exact-ownership tests**

```java
@Test
void staleBindingCannotRemoveNewBinding() {
    ConnectionBinding old = binding("42", "old", oldChannel);
    ConnectionBinding current = binding("42", "current", currentChannel);
    manager.register(old);
    manager.register(current);
    assertFalse(manager.remove(old));
    assertSame(currentChannel, manager.getChannelByUserId("42"));
}

@Test
void closeRequiresMatchingConnectionId() {
    manager.register(binding("42", "current", currentChannel));
    assertFalse(manager.closeIfMatches("42", "old"));
    verify(currentChannel, never()).close();
    assertTrue(manager.closeIfMatches("42", "current"));
    verify(currentChannel).close();
}
```

```java
@Test
void replacementReturnsPreviousAndKeepsExactReverseLookup() {
    ConnectionBinding old = binding("42", "old", oldChannel);
    ConnectionBinding current = binding("42", "current", currentChannel);
    assertNull(manager.register(old));
    assertSame(old, manager.register(current));
    assertSame(old, manager.findByChannel(oldChannel).get());
    assertTrue(manager.close(old));
    assertSame(current, manager.findByUserId("42").get());
}
```

- [ ] **Step 2: Run the test and verify red**

```powershell
mvn -o -f RealTimeCommunicationService/pom.xml -Dtest=ChannelManagerTest test
```

Expected: compilation fails because `ConnectionBinding` and the new instance API do not exist.

- [ ] **Step 3: Convert `ChannelManager` into an injected singleton component**

```java
@Component
public final class ChannelManager {
    private final ConcurrentHashMap<String, ConnectionBinding> byUser = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Channel, ConnectionBinding> byChannel = new ConcurrentHashMap<>();

    public ConnectionBinding register(ConnectionBinding binding) {
        byChannel.put(binding.getChannel(), binding);
        return byUser.put(binding.getUserId(), binding);
    }

    public boolean remove(ConnectionBinding binding) {
        boolean removed = byUser.remove(binding.getUserId(), binding);
        byChannel.remove(binding.getChannel(), binding);
        return removed;
    }

    public boolean close(ConnectionBinding binding) {
        byUser.remove(binding.getUserId(), binding);
        boolean tracked = byChannel.remove(binding.getChannel(), binding);
        if (tracked) binding.getChannel().close();
        return tracked;
    }

    public boolean closeIfMatches(String userId, String connectionId) {
        ConnectionBinding binding = byUser.get(userId);
        if (binding == null || !binding.getConnectionId().equals(connectionId)) return false;
        if (!byUser.remove(userId, binding)) return false;
        byChannel.remove(binding.getChannel(), binding);
        binding.getChannel().close();
        return true;
    }
}
```

Add instance-form compatibility methods for the current handler. `addUserChannel` creates one binding using `channel.id().asLongText()` as its temporary connection ID and registers both maps; `addChannelUser` is idempotent when that binding already exists. The two legacy remove methods preserve the current call order without removing a replacement binding.

Update `NettyMessageService`, `PerfMetricsController`, and `MessageInboundHandler` to constructor-inject the component. For this intermediate compiling state, have `NettyServer` inject `ChannelManager` and construct `new MessageInboundHandler(redisTemplate, channelManager)`; Task 6 will replace that temporary constructor path with the final injected lifecycle handler and delete the compatibility methods. Remove every static `ChannelManager` call. Do not change push payload behavior or performance metric names.

- [ ] **Step 4: Run focused tests and RTC compilation**

```powershell
mvn -o -f RealTimeCommunicationService/pom.xml -Dtest=ChannelManagerTest test
mvn -o -f RealTimeCommunicationService/pom.xml -DskipTests compile
```

Expected: the ownership tests pass and the module compiles after all static call sites are removed.

- [ ] **Step 5: Commit local ownership tracking**

```powershell
git add RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/websocket/ConnectionBinding.java RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/websocket/ChannelManager.java RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/websocket/MessageInboundHandler.java RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/websocket/NettyServer.java RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/service/impl/NettyMessageService.java RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/perf/PerfMetricsController.java RealTimeCommunicationService/src/test/java/com/shanyangcode/infinitechat/realtimecommunicationservice/websocket/ChannelManagerTest.java
git commit -m "refactor(rtc): track exact websocket connection ownership"
```

---

### Task 5: Idempotent Cross-Node Takeover API

**Files:**
- Create: `RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/data/connection/ConnectionTakeoverRequest.java`
- Create: `RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/controller/InternalConnectionController.java`
- Create: `RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/routing/ConnectionTakeoverProperties.java`
- Create: `RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/routing/ConnectionTakeoverClient.java`
- Create: `RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/routing/RoutingConfiguration.java`
- Test: `RealTimeCommunicationService/src/test/java/com/shanyangcode/infinitechat/realtimecommunicationservice/controller/InternalConnectionControllerTest.java`
- Test: `RealTimeCommunicationService/src/test/java/com/shanyangcode/infinitechat/realtimecommunicationservice/routing/ConnectionTakeoverClientTest.java`

**Interfaces:**
- Consumes: Task 4 `ChannelManager#closeIfMatches`.
- Produces: `POST /internal/connections/takeover` with JSON `{ "userId": "42", "connectionId": "old-id" }`.
- Produces: `ConnectionTakeoverProperties` bound to `infinitechat.routing.takeover`, with defaults `connectTimeout = 1s` and `readTimeout = 2s`.
- Produces: `boolean requestTakeover(OnlineRoute oldRoute, String userId)`; `true` for any completed 2xx request, `false` for timeout, transport error, or non-2xx response.

- [ ] **Step 1: Write failing controller and client tests**

```java
@Test
void staleTakeoverIsIdempotent() {
    when(channelManager.closeIfMatches("42", "old-id")).thenReturn(false);
    Result<?> result = controller.takeover(new ConnectionTakeoverRequest("42", "old-id"));
    assertEquals(200, result.getCode());
    verify(channelManager).closeIfMatches("42", "old-id");
}

@Test
void clientTargetsOldEndpointAndSendsExactConnection() {
    when(restOperations.postForEntity(eq(URI.create("http://10.0.0.1:8083/internal/connections/takeover")),
            any(ConnectionTakeoverRequest.class), eq(Void.class)))
            .thenReturn(new ResponseEntity<Void>(HttpStatus.OK));
    assertTrue(client.requestTakeover(oldRoute, "42"));
}
```

```java
@Test
void blankTakeoverFieldsReturnValidationBodyCode() throws Exception {
    MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler()).build();
    mvc.perform(post("/internal/connections/takeover")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userId\":\"\",\"connectionId\":\"\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400));
}

@Test
void transportFailureReturnsFalse() {
    when(restOperations.postForEntity(any(URI.class), any(), eq(Void.class)))
            .thenThrow(new ResourceAccessException("timeout"));
    assertFalse(client.requestTakeover(oldRoute, "42"));
}
```

The existing global exception handler returns validation errors in the `Result.code` field while keeping the HTTP response convention unchanged.

- [ ] **Step 2: Run the tests and verify red**

```powershell
mvn -o -f RealTimeCommunicationService/pom.xml -Dtest=InternalConnectionControllerTest,ConnectionTakeoverClientTest test
```

Expected: compilation fails because the request, controller, and client do not exist.

- [ ] **Step 3: Implement the exact-connection API and bounded client**

```java
@PostMapping("/internal/connections/takeover")
public Result<?> takeover(@Valid @RequestBody ConnectionTakeoverRequest request) {
    channelManager.closeIfMatches(request.getUserId(), request.getConnectionId());
    return Result.OK(null);
}
```

Use `@NotBlank` on both request fields. Annotate `ConnectionTakeoverProperties` with `@Component` and `@ConfigurationProperties(prefix = "infinitechat.routing.takeover")`, default its durations to 1 and 2 seconds, and reject non-positive values in `@PostConstruct`. In `RoutingConfiguration`, construct a dedicated, named `RestOperations` bean from `SimpleClientHttpRequestFactory`, using these properties for both timeouts; do not change global HTTP clients. Constructor-inject that bean into the client so tests can supply a mock.

```java
public boolean requestTakeover(OnlineRoute oldRoute, String userId) {
    URI uri = URI.create(oldRoute.getEndpoint() + "/internal/connections/takeover");
    try {
        ResponseEntity<Void> response = restOperations.postForEntity(
                uri, new ConnectionTakeoverRequest(userId, oldRoute.getConnectionId()), Void.class);
        return response.getStatusCode().is2xxSuccessful();
    } catch (RestClientException ex) {
        log.warn("Failed to take over user {} from node {}", userId, oldRoute.getNodeId());
        return false;
    }
}
```

Keep this URI outside all Gateway route predicates so it is service-network-only in the MVP.

- [ ] **Step 4: Run focused tests and commit**

```powershell
mvn -o -f RealTimeCommunicationService/pom.xml -Dtest=InternalConnectionControllerTest,ConnectionTakeoverClientTest test
git add RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/data/connection RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/controller/InternalConnectionController.java RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/routing/ConnectionTakeoverProperties.java RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/routing/ConnectionTakeoverClient.java RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/routing/RoutingConfiguration.java RealTimeCommunicationService/src/test/java/com/shanyangcode/infinitechat/realtimecommunicationservice/controller/InternalConnectionControllerTest.java RealTimeCommunicationService/src/test/java/com/shanyangcode/infinitechat/realtimecommunicationservice/routing/ConnectionTakeoverClientTest.java
git commit -m "feat(rtc): add idempotent connection takeover"
```

Expected: both focused test classes pass before commit.

---

### Task 6: RTC Connection Lifecycle Integration

**Files:**
- Create: `RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/routing/LocalNode.java`
- Modify: `RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/routing/RoutingConfiguration.java`
- Create: `RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/routing/ConnectionLifecycleService.java`
- Modify: `RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/websocket/MessageInboundHandler.java:29-181`
- Modify: `RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/websocket/NettyServer.java:30-90`
- Modify: `RealTimeCommunicationService/src/main/resources/application.yml`
- Modify: `RealTimeCommunicationService/src/main/resources/application-perf.yml`
- Test: `RealTimeCommunicationService/src/test/java/com/shanyangcode/infinitechat/realtimecommunicationservice/routing/ConnectionLifecycleServiceTest.java`

**Interfaces:**
- Consumes: Tasks 3-5 route registry, channel manager, and takeover client.
- Produces: `LocalNode(String nodeId, String endpoint)` bean with node ID `<host>:<netty.port>` and endpoint override or `http://<host>:<server.port>`; lifecycle code creates `OnlineRoute` by adding a generated connection ID.
- Produces: `boolean establish(String userId, Channel channel)`, `boolean heartbeat(Channel channel)`, and `void disconnect(Channel channel)`.
- Produces: one injected, sharable `MessageInboundHandler` used by `NettyServer`.

- [ ] **Step 1: Write failing lifecycle tests**

```java
@Test
void losingOwnershipAfterRegistrationClosesNewConnection() {
    when(routes.claim(eq("42"), any(OnlineRoute.class))).thenReturn(Optional.empty());
    when(routes.isOwner(eq("42"), anyString())).thenReturn(false);
    assertFalse(lifecycle.establish("42", newChannel));
    verify(newChannel).close();
    assertNull(channels.getChannelByUserId("42"));
}

@Test
void remotePreviousOwnerReceivesExactTakeover() {
    OnlineRoute old = new OnlineRoute("10.0.0.1:9000", "http://10.0.0.1:8083", "old-id");
    when(routes.claim(eq("42"), any(OnlineRoute.class))).thenReturn(Optional.of(old));
    when(routes.isOwner(eq("42"), anyString())).thenReturn(true);
    assertTrue(lifecycle.establish("42", newChannel));
    verify(takeover).requestTakeover(old, "42");
}

@Test
void heartbeatFailureClosesConnection() {
    ConnectionBinding binding = binding("42", "current", channel);
    channels.register(binding);
    when(routes.renew("42", "current")).thenReturn(false);
    assertFalse(lifecycle.heartbeat(channel));
    verify(channel).close();
}

@Test
void staleDisconnectCannotDeleteCurrentRoute() {
    ConnectionBinding stale = binding("42", "old", oldChannel);
    channels.register(stale);
    channels.register(binding("42", "current", currentChannel));
    lifecycle.disconnect(oldChannel);
    verify(routes).release("42", "old");
    assertSame(currentChannel, channels.getChannelByUserId("42"));
}
```

```java
@Test
void sameUserEstablishmentsAreSerializedWithinOneNode() throws Exception {
    CountDownLatch firstClaimEntered = new CountDownLatch(1);
    CountDownLatch releaseFirstClaim = new CountDownLatch(1);
    AtomicInteger calls = new AtomicInteger();
    when(routes.claim(eq("42"), any(OnlineRoute.class))).thenAnswer(invocation -> {
        if (calls.getAndIncrement() == 0) {
            firstClaimEntered.countDown();
            assertTrue(releaseFirstClaim.await(2, TimeUnit.SECONDS));
        }
        return Optional.empty();
    });
    when(routes.isOwner(eq("42"), anyString())).thenReturn(true);

    ExecutorService pool = Executors.newFixedThreadPool(2);
    Future<Boolean> first = pool.submit(() -> lifecycle.establish("42", firstChannel));
    assertTrue(firstClaimEntered.await(2, TimeUnit.SECONDS));
    Future<Boolean> second = pool.submit(() -> lifecycle.establish("42", secondChannel));
    assertEquals(1, calls.get(), "second claim must wait behind the same-user stripe");
    releaseFirstClaim.countDown();
    assertTrue(first.get(2, TimeUnit.SECONDS));
    assertTrue(second.get(2, TimeUnit.SECONDS));
    assertSame(secondChannel, channels.getChannelByUserId("42"));
    pool.shutdownNow();
}
```

- [ ] **Step 2: Run the test and verify red**

```powershell
mvn -o -f RealTimeCommunicationService/pom.xml -Dtest=ConnectionLifecycleServiceTest test
```

Expected: compilation fails because `ConnectionLifecycleService` and `LocalNode` do not exist.

- [ ] **Step 3: Implement lifecycle orchestration**

Use 256 fixed `ReentrantLock` stripes selected by `userId.hashCode() & 255`; never retain a growing map of user locks. Extend `RoutingConfiguration` to create `LocalNode` from the local host address, `netty.port`, `server.port`, and optional advertised endpoint.

```java
public boolean establish(String userId, Channel channel) {
    Lock lock = lockFor(userId);
    lock.lock();
    String connectionId = UUID.randomUUID().toString();
    ConnectionBinding binding = null;
    try {
        OnlineRoute current = new OnlineRoute(localNode.getNodeId(), localNode.getEndpoint(), connectionId);
        Optional<OnlineRoute> oldRoute = routes.claim(userId, current);
        binding = new ConnectionBinding(userId, connectionId, channel);
        ConnectionBinding previousLocal = channels.register(binding);
        if (!routes.isOwner(userId, connectionId)) {
            channels.remove(binding);
            channel.close();
            return false;
        }
        closePrevious(userId, oldRoute, previousLocal);
        return true;
    } catch (RuntimeException ex) {
        if (binding != null) channels.remove(binding);
        try {
            routes.release(userId, connectionId);
        } catch (RuntimeException cleanupFailure) {
            log.warn("Failed to release route after setup failure for user {}", userId);
        }
        channel.close();
        return false;
    } finally {
        lock.unlock();
    }
}
```

After the new binding passes the Redis ownership recheck, `closePrevious` calls `channels.close(previousLocal)` whenever `register` returned a different prior local binding; the exact object comparison prevents closing the new Channel even if Redis had already expired the old route. Independently, when the returned old Redis route belongs to a different node, call `ConnectionTakeoverClient` with that route's exact connection ID. Never close by user ID alone.

`heartbeat` obtains the binding by Channel, calls `routes.renew`, and closes the Channel when renewal returns false or throws. `disconnect` obtains the binding, removes that exact binding, and calls `routes.release`; a Channel without a binding only closes and performs no Redis call.

- [ ] **Step 4: Inject the lifecycle into Netty**

Convert `MessageInboundHandler` to an injected `@Component` with `@Sharable`. After JWT validation call `lifecycle.establish(userUuid, ctx.channel())`; on heartbeat call `lifecycle.heartbeat(ctx.channel())` before replying; in logout, inactive, idle, and exception paths call one idempotent `lifecycle.disconnect(ctx.channel())` path.

Constructor-inject this single handler into `NettyServer` and replace:

```java
pipeline.addLast(new MessageInboundHandler(redisTemplate));
```

with:

```java
pipeline.addLast(messageInboundHandler);
```

Remove direct online-route Redis access and `InetAddress` lookup from `MessageInboundHandler`. Keep JWT semantics and WebSocket message formats unchanged.

- [ ] **Step 5: Add exact RTC configuration**

```yaml
infinitechat:
  routing:
    online-route:
      ttl: 10m
      advertised-http-endpoint: ${RTC_ADVERTISED_HTTP_ENDPOINT:}
    takeover:
      connect-timeout: 1s
      read-timeout: 2s
```

Put the same keys in `application-perf.yml`, with environment overrides and safe local defaults. Validate positive TTL and timeouts at startup.

- [ ] **Step 6: Run lifecycle tests, all RTC tests, and commit**

```powershell
mvn -o -f RealTimeCommunicationService/pom.xml -Dtest=ConnectionLifecycleServiceTest,OnlineRouteRegistryTest,ChannelManagerTest,InternalConnectionControllerTest,ConnectionTakeoverClientTest test
mvn -o -f RealTimeCommunicationService/pom.xml test
git add RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/routing/LocalNode.java RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/routing/RoutingConfiguration.java RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/routing/ConnectionLifecycleService.java RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/websocket/MessageInboundHandler.java RealTimeCommunicationService/src/main/java/com/shanyangcode/infinitechat/realtimecommunicationservice/websocket/NettyServer.java RealTimeCommunicationService/src/main/resources/application.yml RealTimeCommunicationService/src/main/resources/application-perf.yml RealTimeCommunicationService/src/test/java/com/shanyangcode/infinitechat/realtimecommunicationservice/routing/ConnectionLifecycleServiceTest.java
git commit -m "feat(rtc): enforce routed websocket ownership lifecycle"
```

Expected: focused tests and the RTC module suite pass; the commit contains no unrelated files.

---

### Task 7: Adapt Directed Push Callers to Redis Hash Endpoints

**Files:**
- Create: `MessageingService/src/main/java/com/shanyangcode/infinitechat/messageingservice/routing/OnlineRouteLookup.java`
- Modify: `MessageingService/src/main/java/com/shanyangcode/infinitechat/messageingservice/service/impl/MessageServiceImpl.java:60-170`
- Test: `MessageingService/src/test/java/com/shanyangcode/infinitechat/messageingservice/routing/OnlineRouteLookupTest.java`
- Create: `ContanctService/src/main/java/com/shangyangcode/infinitechat/contactservice/routing/OnlineRouteLookup.java`
- Modify: `ContanctService/src/main/java/com/shangyangcode/infinitechat/contactservice/service/impl/PushServiceImpl.java:18-67`
- Test: `ContanctService/src/test/java/com/shangyangcode/infinitechat/contactservice/routing/OnlineRouteLookupTest.java`

**Interfaces:**
- Consumes: Redis Hash `user:session:{userId}` field `endpoint` from Task 3.
- Produces in each module: `Optional<String> findEndpoint(Long userId)` and `Optional<String> resolveUrl(Long userId, String path)`.
- Messaging single-chat and Contact notification code consume the complete base endpoint; neither appends hard-coded host or port.

- [ ] **Step 1: Write failing lookup tests in both modules**

```java
@Test
void readsEndpointFromOnlineRouteHash() {
    when(redis.<String, String>opsForHash().get("user:session:42", "endpoint"))
            .thenReturn("http://10.0.0.2:8083");
    assertEquals("http://10.0.0.2:8083", lookup.findEndpoint(42L).get());
}

@Test
void blankEndpointMeansOffline() {
    when(redis.<String, String>opsForHash().get("user:session:42", "endpoint"))
            .thenReturn(" ");
    assertFalse(lookup.findEndpoint(42L).isPresent());
}

@Test
void resolvesPathWithoutHardCodedHostOrDuplicateSlash() {
    when(redis.<String, String>opsForHash().get("user:session:42", "endpoint"))
            .thenReturn("http://10.0.0.2:8083/");
    assertEquals("http://10.0.0.2:8083/api/v1/message/user/",
            lookup.resolveUrl(42L, "/api/v1/message/user/").get());
}
```

Use `StringRedisTemplate`, not the JSON-valued generic `RedisTemplate`, so the Hash field is read as the exact String written by RTC.

- [ ] **Step 2: Run both lookup tests and verify red**

```powershell
mvn -o -f MessageingService/pom.xml -Dtest=OnlineRouteLookupTest test
mvn -o -f ContanctService/pom.xml -Dtest=OnlineRouteLookupTest test
```

Expected: each module fails compilation because its lookup class does not exist.

- [ ] **Step 3: Implement the two small lookup adapters**

```java
@Component
public final class OnlineRouteLookup {
    private static final String KEY_PREFIX = "user:session:";
    private static final String ENDPOINT_FIELD = "endpoint";
    private final StringRedisTemplate redis;

    public Optional<String> findEndpoint(Long userId) {
        String endpoint = redis.<String, String>opsForHash()
                .get(KEY_PREFIX + userId, ENDPOINT_FIELD);
        return Optional.ofNullable(endpoint).map(String::trim).filter(value -> !value.isEmpty());
    }

    public Optional<String> resolveUrl(Long userId, String path) {
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return findEndpoint(userId).map(endpoint ->
                endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) + normalizedPath
                        : endpoint + normalizedPath);
    }
}
```

Keep one copy in each existing standalone Maven module; do not introduce a shared module for this MVP.

- [ ] **Step 4: Replace bare-IP URL construction**

In `MessageServiceImpl`, constructor-inject its `OnlineRouteLookup`, remove the online-route `RedisTemplate` dependency, and change only the single-chat path:

```java
Optional<String> targetUrl = onlineRouteLookup.resolveUrl(
        sendMsgRequest.getReceiveUserId(), ConfigEnum.MSG_URL.getValue());
if (targetUrl.isPresent()) {
    Request request = new Request.Builder()
            .url(targetUrl.get())
            .post(requestBody)
            .build();
    executeHttpRequest(request);
} else {
    log.info("接收者已下线: {}", receiveUserId);
}
```

Leave group broadcast behavior unchanged.

In `PushServiceImpl`, inject its lookup and use:

```java
Optional<String> targetUrl = onlineRouteLookup.resolveUrl(userId, urlEndpoint + userId);
if (!targetUrl.isPresent()) {
    log.info(offlineLogMsg);
    return;
}
Request request = new Request.Builder()
        .url(targetUrl.get())
        .post(requestBody)
        .build();
```

- [ ] **Step 5: Run caller tests and module suites**

```powershell
mvn -o -f MessageingService/pom.xml -Dtest=OnlineRouteLookupTest test
mvn -o -f MessageingService/pom.xml test
mvn -o -f ContanctService/pom.xml -Dtest=OnlineRouteLookupTest test
mvn -o -f ContanctService/pom.xml test
```

Expected: both lookup tests and both module suites pass; single-user push URLs contain the stored endpoint and no hard-coded `:8083`.

- [ ] **Step 6: Commit route consumers**

```powershell
git add MessageingService/src/main/java/com/shanyangcode/infinitechat/messageingservice/routing/OnlineRouteLookup.java MessageingService/src/main/java/com/shanyangcode/infinitechat/messageingservice/service/impl/MessageServiceImpl.java MessageingService/src/test/java/com/shanyangcode/infinitechat/messageingservice/routing/OnlineRouteLookupTest.java ContanctService/src/main/java/com/shangyangcode/infinitechat/contactservice/routing/OnlineRouteLookup.java ContanctService/src/main/java/com/shangyangcode/infinitechat/contactservice/service/impl/PushServiceImpl.java ContanctService/src/test/java/com/shangyangcode/infinitechat/contactservice/routing/OnlineRouteLookupTest.java
git commit -m "feat(routing): resolve directed pushes from online routes"
```

---

### Task 8: Runbook, Full Verification, and Scope Audit

**Files:**
- Create: `docs/consistent-hash-routing-runbook.md`

**Interfaces:**
- Consumes: all preceding tasks.
- Produces: an operator-safe maintenance migration and manual Nacos/Redis multi-node validation procedure.

- [ ] **Step 1: Write the runbook before final verification**

Document these exact sections:

```markdown
# Consistent Hash Routing Runbook

## Preconditions
- Stop Gateway, RTC, Messaging, and Contact instances before migrating String routes.
- Inspect `user:session:*` and confirm the prefix contains only ephemeral online connection routes.

## Migration
- Use Redis SCAN with MATCH `user:session:*`; do not use `KEYS` in a shared or production Redis.
- Review the matched keys, then delete only those confirmed online-route keys during the maintenance window.
- Start RTC instances, then Gateway, Messaging, and Contact services; reconnect clients to rebuild Hash routes.

## Multi-node checks
1. Start two RTC processes with distinct Netty ports and HTTP endpoints registered in Nacos.
2. Reconnect the same `userUuid` and verify stable selection while membership is unchanged.
3. Add one RTC node and verify existing sockets stay connected while only part of reconnecting users move.
4. Reconnect one user onto a different node and verify exact old-connection takeover.
5. Trigger stale disconnect and verify the new Redis `connectionId` remains.
6. Stop a node ungracefully and verify its routes expire after the configured TTL.

## MVP limitations
- No mixed-version rolling deployment.
- No strong single-connection guarantee during network partitions.
- No retry queue for failed takeover calls.
- Group and Moment broadcast routing is unchanged.
```

Include environment-variable examples for `RTC_ADVERTISED_HTTP_ENDPOINT` but no credentials or real infrastructure addresses.

- [ ] **Step 2: Run whitespace and secret checks**

```powershell
git diff --check
git diff --unified=0 5940dc5..HEAD | rg -n "password:|token:|secret:"
git diff --unified=0 | rg -n "password:|token:|secret:"
```

Expected: `git diff --check` exits successfully. The two diff-only searches expose no newly added secret value; configuration placeholders and field names are allowed. Do not scan or print unchanged configuration values because the repository already contains legacy credentials outside this feature's scope.

- [ ] **Step 3: Run all affected module tests offline**

```powershell
mvn -o -f GateWay/pom.xml test
mvn -o -f RealTimeCommunicationService/pom.xml test
mvn -o -f MessageingService/pom.xml test
mvn -o -f ContanctService/pom.xml test
```

Expected: all four commands exit 0 with zero test failures. If Maven reports a missing offline artifact, stop and request user permission before any online retry or write to the global Maven repository.

- [ ] **Step 4: Verify the implementation against the spec**

```powershell
rg -n "userUuid|virtual-nodes|SHA-256|NettyService" GateWay/src/main GateWay/src/test
rg -n "nodeId|endpoint|connectionId|PEXPIRE|closeIfMatches" RealTimeCommunicationService/src/main RealTimeCommunicationService/src/test
rg -n "opsForHash|endpoint" MessageingService/src/main ContanctService/src/main
git diff --name-only 5940dc5..HEAD -- "*pom.xml"
git status --short
```

Check every spec completion criterion against concrete code and test output. Confirm `.claude/` remains untracked and unstaged, group/Moment broadcast code is unchanged, and the POM diff command prints no path because no dependency changed.

- [ ] **Step 5: Request code review and fix only verified findings**

Invoke `superpowers:requesting-code-review`. Provide the design spec, this plan, the commit range, and all four Maven results. Address confirmed correctness issues, then rerun the affected focused test and its full module suite.

- [ ] **Step 6: Commit documentation and any review fixes**

```powershell
git add docs/consistent-hash-routing-runbook.md
git commit -m "docs: add consistent hash routing runbook"
```

Before claiming completion, invoke `superpowers:verification-before-completion` and rerun the exact verification commands that support the final status.
