# Consistent Hash Routing Runbook

## Preconditions

- Stop Gateway, RTC, Messaging, and Contact instances before migrating String routes to Hash routes.
- Inspect `user:session:*` and confirm the prefix contains only ephemeral online connection routes.
- Confirm the maintenance window is active before reviewing or deleting any matched online-route keys.
- Prepare RTC advertised HTTP endpoint placeholders for each RTC process, for example:
  - `RTC_ADVERTISED_HTTP_ENDPOINT=http://<rtc-node-a-host>:<rtc-node-a-http-port>`
  - `RTC_ADVERTISED_HTTP_ENDPOINT=http://<rtc-node-b-host>:<rtc-node-b-http-port>`

## Migration

- Use Redis `SCAN MATCH user:session:*`; do not use `KEYS` in a shared or production Redis.
- Review the matched keys, then delete only those confirmed online-route keys during the maintenance window.
- Do not delete keys outside the `user:session:*` online-route prefix.
- Start RTC instances first, then Gateway, Messaging, and Contact services.
- Reconnect clients to rebuild Hash routes containing `nodeId`, `endpoint`, and `connectionId`.

## Multi-node checks

1. Start two RTC processes with distinct Netty ports and HTTP endpoints registered in Nacos.
2. Configure each process with its own advertised HTTP endpoint, for example `RTC_ADVERTISED_HTTP_ENDPOINT=http://<rtc-node-a-host>:<rtc-node-a-http-port>` and `RTC_ADVERTISED_HTTP_ENDPOINT=http://<rtc-node-b-host>:<rtc-node-b-http-port>`.
3. Reconnect the same `userUuid` and verify stable selection while membership is unchanged.
4. Add one RTC node and verify existing sockets stay connected while only part of reconnecting users move.
5. Reconnect one user onto a different node and verify exact old-connection takeover.
6. Trigger stale disconnect and verify the new Redis `connectionId` remains.
7. Stop a node ungracefully and verify its routes expire after the configured TTL.

## MVP limitations

- No mixed-version rolling deployment.
- No strong single-connection guarantee during network partitions.
- No retry queue for failed takeover calls.
- Group and Moment broadcast routing is unchanged.
