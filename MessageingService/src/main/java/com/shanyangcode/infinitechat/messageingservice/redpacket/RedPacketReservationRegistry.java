package com.shanyangcode.infinitechat.messageingservice.redpacket;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Redis-backed state machine for red-packet inventory reservations. */
@Component
public class RedPacketReservationRegistry {

    private static final String INVENTORY_PREFIX = "red_packet:count:";
    private static final String RESERVATION_PREFIX = "red_packet:reservation:";
    private static final String PENDING_KEY = "red_packet:pending";
    private static final String STATE_PENDING = "1";
    private static final String STATE_CONFIRMED = "2";
    private static final String STATE_RELEASED = "3";

    private static final long REPLY_RESERVED = 1L;
    private static final long REPLY_PENDING = 2L;
    private static final long REPLY_EMPTY = 3L;
    private static final long REPLY_ALREADY_CLAIMED = 4L;

    private static final String RESERVE_LUA =
            "local state = redis.call('HGET', KEYS[2], 'state')\n"
                    + "if state == '1' then return {2} end\n"
                    + "if state == '2' then return {4} end\n"
                    + "local count = redis.call('GET', KEYS[1])\n"
                    + "if not count or tonumber(count) <= 0 then return {3} end\n"
                    + "redis.call('DECR', KEYS[1])\n"
                    + "redis.call('HSET', KEYS[2], 'state', '1', 'token', ARGV[1], 'createdAt', ARGV[2])\n"
                    + "local ttl = redis.call('TTL', KEYS[1])\n"
                    + "if ttl >= 0 then redis.call('EXPIRE', KEYS[2], ttl) end\n"
                    + "redis.call('ZADD', KEYS[3], ARGV[2], ARGV[3])\n"
                    + "return {1, ARGV[1], ARGV[2]}";

    private static final String CONFIRM_LUA =
            "if redis.call('HGET', KEYS[1], 'state') ~= '1' then redis.call('ZREM', KEYS[2], ARGV[2]) return 0 end\n"
                    + "if redis.call('HGET', KEYS[1], 'token') ~= ARGV[1] then redis.call('ZREM', KEYS[2], ARGV[2]) return 0 end\n"
                    + "redis.call('HSET', KEYS[1], 'state', '2')\n"
                    + "redis.call('ZREM', KEYS[2], ARGV[2])\n"
                    + "return 1";

    private static final String RELEASE_LUA =
            "if redis.call('HGET', KEYS[2], 'state') ~= '1' then redis.call('ZREM', KEYS[3], ARGV[2]) return 0 end\n"
                    + "if redis.call('HGET', KEYS[2], 'token') ~= ARGV[1] then redis.call('ZREM', KEYS[3], ARGV[2]) return 0 end\n"
                    + "if redis.call('EXISTS', KEYS[1]) == 1 then redis.call('INCR', KEYS[1]) end\n"
                    + "redis.call('HSET', KEYS[2], 'state', '3')\n"
                    + "redis.call('ZREM', KEYS[3], ARGV[2])\n"
                    + "return 1";

    private final StringRedisTemplate redisTemplate;
    private final Clock clock;
    private final DefaultRedisScript<List> reserveScript;
    private final DefaultRedisScript<Long> confirmScript;
    private final DefaultRedisScript<Long> releaseScript;

    @Autowired
    public RedPacketReservationRegistry(StringRedisTemplate redisTemplate) {
        this(redisTemplate, Clock.systemUTC());
    }

    public RedPacketReservationRegistry(StringRedisTemplate redisTemplate, Clock clock) {
        this.redisTemplate = redisTemplate;
        this.clock = clock;
        this.reserveScript = scriptList(RESERVE_LUA);
        this.confirmScript = new DefaultRedisScript<Long>(CONFIRM_LUA, Long.class);
        this.releaseScript = new DefaultRedisScript<Long>(RELEASE_LUA, Long.class);
    }

    public ReservationResult reserve(long redPacketId, long userId) {
        String token = UUID.randomUUID().toString();
        long createdAt = clock.millis();
        List result = redisTemplate.execute(reserveScript, reservationKeys(redPacketId, userId), token,
                Long.toString(createdAt), pendingMember(redPacketId, userId, token));
        long reply = replyCode(result);
        if (reply == REPLY_RESERVED) {
            return ReservationResult.reserved(new RedPacketReservation(redPacketId, userId,
                    String.valueOf(result.get(1)), asLong(result.get(2))));
        }
        if (reply == REPLY_PENDING) {
            return ReservationResult.of(ReservationResult.Status.PENDING);
        }
        if (reply == REPLY_EMPTY) {
            return ReservationResult.of(ReservationResult.Status.EMPTY);
        }
        if (reply == REPLY_ALREADY_CLAIMED) {
            return ReservationResult.of(ReservationResult.Status.ALREADY_CLAIMED);
        }
        throw new IllegalStateException("Unexpected reservation script reply: " + result);
    }

    public boolean confirm(RedPacketReservation reservation) {
        Long result = redisTemplate.execute(confirmScript,
                Arrays.asList(reservationKey(reservation.getRedPacketId(), reservation.getUserId()), PENDING_KEY),
                reservation.getToken(), pendingMember(reservation));
        return Long.valueOf(1L).equals(result);
    }

    public boolean release(RedPacketReservation reservation) {
        Long result = redisTemplate.execute(releaseScript, reservationKeys(reservation.getRedPacketId(), reservation.getUserId()),
                reservation.getToken(), pendingMember(reservation));
        return Long.valueOf(1L).equals(result);
    }

    public List<RedPacketReservation> findExpiredPending(long cutoffEpochMillis, int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        Set<TypedTuple<String>> members = redisTemplate.opsForZSet().rangeByScoreWithScores(
                PENDING_KEY, Double.NEGATIVE_INFINITY, (double) cutoffEpochMillis, 0, limit);
        if (members == null || members.isEmpty()) {
            return Collections.emptyList();
        }
        List<RedPacketReservation> reservations = new ArrayList<RedPacketReservation>();
        for (TypedTuple<String> member : members) {
            RedPacketReservation reservation = parsePendingMember(member);
            if (reservation != null) {
                reservations.add(reservation);
            } else {
                redisTemplate.opsForZSet().remove(PENDING_KEY, member.getValue());
            }
        }
        return reservations;
    }

    private static DefaultRedisScript<List> scriptList(String scriptText) {
        DefaultRedisScript<List> script = new DefaultRedisScript<List>();
        script.setScriptText(scriptText);
        script.setResultType(List.class);
        return script;
    }

    private static long replyCode(List result) {
        if (result == null || result.isEmpty()) {
            throw new IllegalStateException("Reservation script returned no reply");
        }
        return asLong(result.get(0));
    }

    private static long asLong(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : Long.parseLong(String.valueOf(value));
    }

    private static RedPacketReservation parsePendingMember(TypedTuple<String> tuple) {
        String member = tuple.getValue();
        Double score = tuple.getScore();
        if (member == null || score == null) {
            return null;
        }
        int first = member.indexOf(':');
        int second = first < 0 ? -1 : member.indexOf(':', first + 1);
        if (first <= 0 || second <= first + 1 || second == member.length() - 1) {
            return null;
        }
        try {
            return new RedPacketReservation(Long.parseLong(member.substring(0, first)),
                    Long.parseLong(member.substring(first + 1, second)), member.substring(second + 1), score.longValue());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static List<String> reservationKeys(long redPacketId, long userId) {
        return Arrays.asList(inventoryKey(redPacketId), reservationKey(redPacketId, userId), PENDING_KEY);
    }

    private static String inventoryKey(long redPacketId) {
        return INVENTORY_PREFIX + redPacketId;
    }

    private static String reservationKey(long redPacketId, long userId) {
        return RESERVATION_PREFIX + "{" + redPacketId + "}:" + userId;
    }

    private static String pendingMember(long redPacketId, long userId, String token) {
        return redPacketId + ":" + userId + ":" + token;
    }

    private static String pendingMember(RedPacketReservation reservation) {
        return pendingMember(reservation.getRedPacketId(), reservation.getUserId(), reservation.getToken());
    }
}
