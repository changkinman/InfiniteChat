package com.shanyangcode.infinitechat.messageingservice.constants;

import java.math.BigDecimal;

/**
 * 红包相关的常量定义。
 */
public enum RedPacketConstants {
    RED_PACKET_KEY_PREFIX("red_packet:count:"),
    RED_PACKET_CLAIMED_KEY_PREFIX("red_packet:claimed:"),
    RED_PACKET_LUA_SCRIPT(
            "if redis.call('sismember', KEYS[2], ARGV[1]) == 1 then return 3 end " +
                    "local count = redis.call('get', KEYS[1]) " +
                    "if count == false then return 0 end " +
                    "if tonumber(count) <= 0 then return 2 end " +
                    "redis.call('decr', KEYS[1]) " +
                    "redis.call('sadd', KEYS[2], ARGV[1]) " +
                    "local ttl = redis.call('ttl', KEYS[1]) " +
                    "if ttl > 0 then redis.call('expire', KEYS[2], ttl) end " +
                    "return 1"),
    RED_PACKET_RELEASE_LUA_SCRIPT(
            "if redis.call('srem', KEYS[2], ARGV[1]) == 1 then " +
                    "  local ttl = redis.call('ttl', KEYS[2]) " +
                    "  if redis.call('exists', KEYS[1]) == 1 then redis.call('incr', KEYS[1]) " +
                    "  else redis.call('set', KEYS[1], 1); if ttl > 0 then redis.call('expire', KEYS[1], ttl) end end " +
                    "end return 1"),
    RED_PACKET_TYPE_NORMAL("1"),
    RED_PACKET_TYPE_RANDOM("2"),
    WORKED_ID("1"),
    DATACENTER_ID("1"),
    MIN_AMOUNT(new BigDecimal("0.01")),
    RANDOM_MULTIPLIER(new BigDecimal("2")),
    DIVIDE_SCALE(2),
    AMOUNT_SCALE(2),
    RED_PACKET_EXPIRE_HOURS("24"), // 红包过期时间（小时）
    MAX_AMOUNT_PER_PACKET(new BigDecimal("200")), // 单个红包最大金额
    DATE_TIME_FORMAT("MM月dd日 HH:mm");


    private final Object value;

    RedPacketConstants(Object value) {
        this.value = value;
    }

    public String getValue() {
        return value.toString();
    }

    public Long getLongValue() {
        if (value instanceof String) {
            return Long.parseLong((String) value);
        }
        return (Long) value;
    }

    public Integer getIntValue() {
        if (value instanceof String) {
            return Integer.parseInt((String) value);
        }
        return (Integer) value;
    }

    public BigDecimal getBigDecimalValue() {
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        return new BigDecimal(value.toString());
    }

    public Integer getDivideScale() {
        return (Integer) value;
    }

    public String getDateTimeFormat() {
        return value.toString();
    }
}
