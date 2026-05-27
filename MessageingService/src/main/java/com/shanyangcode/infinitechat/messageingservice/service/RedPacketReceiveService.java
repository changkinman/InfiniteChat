package com.shanyangcode.infinitechat.messageingservice.service;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shanyangcode.infinitechat.messageingservice.common.ServiceException;
import com.shanyangcode.infinitechat.messageingservice.constants.BalanceLogType;
import com.shanyangcode.infinitechat.messageingservice.constants.RedPacketConstants;
import com.shanyangcode.infinitechat.messageingservice.constants.RedPacketStatus;
import com.shanyangcode.infinitechat.messageingservice.data.receiveRedPackage.ReceiveRedPacketResponse;
import com.shanyangcode.infinitechat.messageingservice.mapper.BalanceLogMapper;
import com.shanyangcode.infinitechat.messageingservice.mapper.RedPacketMapper;
import com.shanyangcode.infinitechat.messageingservice.mapper.RedPacketReceiveMapper;
import com.shanyangcode.infinitechat.messageingservice.mapper.UserBalanceMapper;
import com.shanyangcode.infinitechat.messageingservice.model.BalanceLog;
import com.shanyangcode.infinitechat.messageingservice.model.RedPacket;
import com.shanyangcode.infinitechat.messageingservice.model.RedPacketReceive;
import com.shanyangcode.infinitechat.messageingservice.model.UserBalance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Objects;

/**
 * 服务类，用于处理红包领取相关的业务逻辑。
 */
@Service
public class RedPacketReceiveService extends ServiceImpl<RedPacketMapper, RedPacket> {

    private final UserBalanceMapper userBalanceMapper;
    private final BalanceLogMapper balanceLogMapper;
    private final RedPacketReceiveMapper redPacketReceiveMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final GetRedPacketService getRedPacketService;

    private final Snowflake snowflake;

    // Redis Lua脚本用于原子性地检查并递减红包数量
    private static final String RED_PACKET_LUA_SCRIPT = RedPacketConstants.RED_PACKET_LUA_SCRIPT.getValue();
    private static final String RED_PACKET_KEY_PREFIX = RedPacketConstants.RED_PACKET_KEY_PREFIX.getValue();
    private static final Integer CLAIMED = RedPacketStatus.CLAIMED.getStatus();

    /**
     * 构造函数，使用构造函数注入依赖。
     */
    @Autowired
    public RedPacketReceiveService(UserBalanceMapper userBalanceMapper,
                                   BalanceLogMapper balanceLogMapper,
                                   RedPacketReceiveMapper redPacketReceiveMapper,
                                   RedisTemplate<String, Object> redisTemplate,
                                   GetRedPacketService getRedPacketService) {
        this.userBalanceMapper = userBalanceMapper;
        this.balanceLogMapper = balanceLogMapper;
        this.redPacketReceiveMapper = redPacketReceiveMapper;
        this.redisTemplate = redisTemplate;
        this.getRedPacketService = getRedPacketService;
        this.snowflake = IdUtil.getSnowflake(
                Integer.parseInt(RedPacketConstants.WORKED_ID.getValue()),
                Integer.parseInt(RedPacketConstants.DATACENTER_ID.getValue()));
    }

    /**
     * 领取红包
     *
     * @param userId      用户ID
     * @param redPacketId 红包ID
     * @return ReceiveRedPacketResponse 红包领取响应
     * @throws ServiceException 业务异常
     */
    @Transactional
    public ReceiveRedPacketResponse receiveRedPacket(Long userId, Long redPacketId) throws ServiceException {

        // 检查用户是否已领取过红包，如果已领取则返回红包详情页
        BigDecimal amount = verifyUserHasNotReceived(redPacketId, userId);
        if (amount != null) {
            return new ReceiveRedPacketResponse(amount, 0);
        }

        // 尝试抢红包
        Integer result = grabRedPacket(redPacketId);
        if (result.equals(CLAIMED)) {
            return new ReceiveRedPacketResponse(null, CLAIMED);
        }

        // 获取红包信息
        RedPacket redPacket = getRedPacketById(redPacketId);

        // 检查红包状态
        Integer status = validateRedPacketStatus(redPacket);
        if (status != 0) {
            return new ReceiveRedPacketResponse(null, status);
        }

        // 计算领取金额
        BigDecimal receivedAmount = computeReceivedAmount(redPacket);

        // 更新红包信息
        updateRedPacketInfo(redPacket, receivedAmount);

        // 插入领取记录
        LocalDateTime receiveTime = logRedPacketReceive(redPacketId, userId, receivedAmount);

        // 更新用户余额
        adjustUserBalance(userId, receivedAmount);

        // 记录余额变动日志
        logBalanceChange(userId, receivedAmount, redPacketId);

        // 构建响应对象
        return new ReceiveRedPacketResponse(receivedAmount, status);
    }

    /**
     * 尝试为用户抢红包，执行Redis Lua脚本。
     *
     * @param redPacketId 红包ID
     * @return Long 抢红包结果
     */
    private Integer grabRedPacket(Long redPacketId) {
        String redPacketCountKey = RED_PACKET_KEY_PREFIX + redPacketId;
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(RED_PACKET_LUA_SCRIPT);
        redisScript.setResultType(Long.class);
        try {
            Long result = redisTemplate.execute(redisScript, Collections.singletonList(redPacketCountKey));
            if (result == null) {
                throw new IllegalStateException("Redis 脚本执行返回 null");
            }
            return result.intValue();
        } catch (Exception e) {
            throw new RuntimeException("执行 Redis Lua 脚本时出错", e);
        }
    }


    /**
     * 获取红包信息，通过ID查询红包。
     *
     * @param redPacketId 红包ID
     * @return RedPacket 红包对象
     * @throws ServiceException 如果红包不存在
     */
    private RedPacket getRedPacketById(Long redPacketId) throws ServiceException {
        RedPacket redPacket = this.getById(redPacketId);
        if (redPacket == null) {
            throw new ServiceException("红包不存在");
        }
        return redPacket;
    }

    /**
     * 验证红包的状态，包括是否过期和剩余数量。
     *
     * @param redPacket 红包对象
     * @throws ServiceException 如果红包已过期或已被领取完毕
     */
    private Integer validateRedPacketStatus(RedPacket redPacket) throws ServiceException {
        if (Objects.equals(redPacket.getStatus(), RedPacketStatus.EXPIRED.getStatus())) {
            return RedPacketStatus.EXPIRED.getStatus();
        }
        if (redPacket.getRemainingCount() <= 0) {
            return RedPacketStatus.CLAIMED.getStatus();
        }
        return 0;
    }

    /**
     * 验证用户是否已领取过该红包。
     *
     * @param redPacketId 红包ID
     * @param userId      用户ID
     * @return 用户已领取金额
     */
    private BigDecimal verifyUserHasNotReceived(Long redPacketId, Long userId) throws ServiceException {
        QueryWrapper<RedPacketReceive> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("red_packet_id", redPacketId).eq("receiver_id", userId);
        RedPacketReceive redPacketReceive = redPacketReceiveMapper.selectOne(queryWrapper);
        if (redPacketReceive == null) {
            return null;
        }
        return redPacketReceive.getAmount();
    }

    /**
     * 计算用户领取的红包金额，基于红包类型和剩余金额。
     *
     * @param redPacket 红包对象
     * @return BigDecimal 领取金额
     * @throws ServiceException 如果红包类型未知
     */
    private BigDecimal computeReceivedAmount(RedPacket redPacket) throws ServiceException {
        if (Objects.equals(redPacket.getRedPacketType(), RedPacketConstants.RED_PACKET_TYPE_NORMAL.getIntValue())) {
            return calculateNormalRedPacket(redPacket);
        } else if (Objects.equals(redPacket.getRedPacketType(), RedPacketConstants.RED_PACKET_TYPE_RANDOM.getIntValue())) {
            return calculateRandomRedPacket(redPacket);
        } else {
            throw new ServiceException("未知的红包类型");
        }
    }

    /**
     * 计算普通红包的领取金额，平均分配。
     *
     * @param redPacket 红包对象
     * @return BigDecimal 领取金额
     */
    private BigDecimal calculateNormalRedPacket(RedPacket redPacket) {
        return redPacket.getTotalAmount()
                .divide(new BigDecimal(redPacket.getTotalCount()), RedPacketConstants.DIVIDE_SCALE.getDivideScale(), RoundingMode.DOWN);
    }

    /**
     * 计算拼手气红包的领取金额，随机分配。
     *
     * @param redPacket 红包对象
     * @return BigDecimal 领取金额
     */
    private BigDecimal calculateRandomRedPacket(RedPacket redPacket) {
        if (redPacket.getRemainingCount() == 1) {
            // 最后一个红包，领取剩余所有金额
            return redPacket.getRemainingAmount();
        } else {
            // 计算最大可领取金额
            BigDecimal maxAmount = redPacket.getRemainingAmount()
                    .divide(new BigDecimal(redPacket.getRemainingCount()), RedPacketConstants.DIVIDE_SCALE.getDivideScale(), RoundingMode.DOWN)
                    .multiply(RedPacketConstants.RANDOM_MULTIPLIER.getBigDecimalValue());
            return generateRandomAmount(RedPacketConstants.MIN_AMOUNT.getBigDecimalValue(), maxAmount);
        }
    }

    /**
     * 生成指定范围内的随机金额。
     *
     * @param min 最小金额
     * @param max 最大金额
     * @return BigDecimal 随机金额
     */
    private BigDecimal generateRandomAmount(BigDecimal min, BigDecimal max) {
        BigDecimal range = max.subtract(min);
        BigDecimal randomInRange = range.multiply(BigDecimal.valueOf(Math.random()));
        BigDecimal randomAmount = min.add(randomInRange).setScale(RedPacketConstants.AMOUNT_SCALE.getDivideScale(), RoundingMode.DOWN);
        return randomAmount.compareTo(min) < 0 ? min : randomAmount;
    }

    /**
     * 更新红包的剩余金额和数量，必要时更新红包状态为已领取完毕。
     *
     * @param redPacket      红包对象
     * @param receivedAmount 领取金额
     * @throws ServiceException 如果更新红包失败
     */
    private void updateRedPacketInfo(RedPacket redPacket, BigDecimal receivedAmount) throws ServiceException {
        redPacket.setRemainingAmount(redPacket.getRemainingAmount().subtract(receivedAmount));
        redPacket.setRemainingCount(redPacket.getRemainingCount() - 1);

        if (redPacket.getRemainingCount() == 0) {
            redPacket.setStatus(RedPacketStatus.CLAIMED.getStatus());
            redisTemplate.delete("red_packet:count:" + redPacket.getRedPacketId());
        }

        boolean updateSuccess = this.updateById(redPacket);
        if (!updateSuccess) {
            throw new ServiceException("更新红包信息失败");
        }
    }

    /**
     * 记录红包领取信息到数据库。
     *
     * @param redPacketId    红包ID
     * @param userId         用户ID
     * @param receivedAmount 领取金额
     * @return LocalDateTime 领取时间
     * @throws ServiceException 如果插入领取记录失败
     */
    private LocalDateTime logRedPacketReceive(Long redPacketId, Long userId, BigDecimal receivedAmount) throws ServiceException {
        RedPacketReceive receive = new RedPacketReceive();
        receive.setRedPacketReceiveId(generateId());
        receive.setRedPacketId(redPacketId);
        receive.setReceiverId(userId);
        receive.setAmount(receivedAmount);
        receive.setReceivedAt(LocalDateTime.now());

        int insertResult = redPacketReceiveMapper.insert(receive);
        if (insertResult != 1) {
            throw new ServiceException("红包领取记录插入失败");
        }
        return receive.getReceivedAt();
    }

    /**
     * 更新用户余额。
     *
     * @param userId         用户ID
     * @param receivedAmount 领取金额
     * @throws ServiceException 如果更新用户余额失败
     */
    private void adjustUserBalance(Long userId, BigDecimal receivedAmount) throws ServiceException {
        UserBalance userBalance = userBalanceMapper.selectById(userId);
        if (userBalance == null) {
            throw new ServiceException("用户余额信息不存在");
        }

        userBalance.setBalance(userBalance.getBalance().add(receivedAmount));
        userBalance.setUpdatedAt(LocalDateTime.now());

        int updateResult = userBalanceMapper.updateById(userBalance);
        if (updateResult != 1) {
            throw new ServiceException("更新用户余额失败");
        }
    }

    /**
     * 记录用户余额变动日志。
     *
     * @param userId         用户ID
     * @param receivedAmount 变动金额
     * @param redPacketId    关联红包ID
     * @throws ServiceException 如果插入余额变动日志失败
     */
    private void logBalanceChange(Long userId, BigDecimal receivedAmount, Long redPacketId) throws ServiceException {
        BalanceLog balanceLog = new BalanceLog();
        balanceLog.setBalanceLogId(generateId());
        balanceLog.setUserId(userId);
        balanceLog.setAmount(receivedAmount);
        balanceLog.setType(BalanceLogType.RECEIVE_RED_PACKET.getType());
        balanceLog.setRelatedId(redPacketId);
        balanceLog.setCreatedAt(LocalDateTime.now());

        int insertResult = balanceLogMapper.insert(balanceLog);
        if (insertResult != 1) {
            throw new ServiceException("记录余额变动日志失败");
        }
    }
/*

    */
/**
     * 构建红包领取响应对象。
     *
     * @param userId         用户ID
     * @param redPacketId    红包ID
     * @param receivedAmount 领取金额
     * @param receiveTime    领取时间
     * @return ReceiveRedPacketResponse 领取响应
     *//*

    private ReceiveRedPacketResponse buildReceiveRedPacketResponse(Long userId, Long redPacketId,
                                                                   BigDecimal receivedAmount, LocalDateTime receiveTime) {
        ReceiveRedPacketResponse response = new ReceiveRedPacketResponse();
        response.setRedPacketId(redPacketId);
        response.setUserId(userId);
        response.setReceivedAmount(receivedAmount);
        response.setReceivedAt(receiveTime.format(DateTimeFormatter.ofPattern(RedPacketConstants.DATE_TIME_FORMAT.getValue())));
        return response;
    }
*/

    /**
     * 生成唯一ID，使用雪花算法。
     *
     * @return Long 唯一ID
     */
    private Long generateId() {
        return snowflake.nextId();
    }
}