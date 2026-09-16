package com.shanyangcode.infinitechat.messageingservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.shanyangcode.infinitechat.messageingservice.constants.RedPacketStatus;
import com.shanyangcode.infinitechat.messageingservice.data.receiveRedPackage.ReceiveRedPacketResponse;
import com.shanyangcode.infinitechat.messageingservice.mapper.BalanceLogMapper;
import com.shanyangcode.infinitechat.messageingservice.mapper.RedPacketMapper;
import com.shanyangcode.infinitechat.messageingservice.mapper.RedPacketReceiveMapper;
import com.shanyangcode.infinitechat.messageingservice.mapper.UserBalanceMapper;
import com.shanyangcode.infinitechat.messageingservice.model.RedPacket;
import com.shanyangcode.infinitechat.messageingservice.model.RedPacketReceive;
import com.shanyangcode.infinitechat.messageingservice.model.UserBalance;
import com.shanyangcode.infinitechat.messageingservice.redpacket.RedPacketReservation;
import com.shanyangcode.infinitechat.messageingservice.redpacket.RedPacketReservationRegistry;
import com.shanyangcode.infinitechat.messageingservice.redpacket.ReservationResult;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class RedPacketReceiveServiceTest {

    private static final Long USER_ID = 42L;
    private static final Long RED_PACKET_ID = 71L;

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void pendingReservationReturnsProcessingResponseWithoutLockingPacket() {
        Fixture fixture = new Fixture();
        when(fixture.receiveMapper.selectByPacketIdAndReceiverId(RED_PACKET_ID, USER_ID)).thenReturn(null);
        when(fixture.registry.reserve(RED_PACKET_ID, USER_ID))
                .thenReturn(ReservationResult.of(ReservationResult.Status.PENDING));

        ReceiveRedPacketResponse response = fixture.service.receiveRedPacket(USER_ID, RED_PACKET_ID);

        assertNull(response.getReceivedAmount());
        assertEquals(Integer.valueOf(4), response.getStatus());
        assertEquals("领取处理中，请稍后重试", response.getMessage());
        verify(fixture.redPacketMapper, never()).selectByIdForUpdate(any(Long.class));
    }

    @Test
    void successfulReceiveConfirmsExactReservationAfterCommit() {
        Fixture fixture = successfulFixture();
        TransactionSynchronizationManager.initSynchronization();

        ReceiveRedPacketResponse response = fixture.service.receiveRedPacket(USER_ID, RED_PACKET_ID);

        assertEquals(new BigDecimal("5.00"), response.getReceivedAmount());
        verify(fixture.registry, never()).confirm(any(RedPacketReservation.class));
        invokeAfterCommit();
        verify(fixture.registry).confirm(same(fixture.reservation));
        verify(fixture.registry, never()).release(any(RedPacketReservation.class));
    }

    @Test
    void rolledBackReceiveReleasesExactReservationAfterCompletion() {
        Fixture fixture = successfulFixture();
        TransactionSynchronizationManager.initSynchronization();

        fixture.service.receiveRedPacket(USER_ID, RED_PACKET_ID);

        verify(fixture.registry, never()).release(any(RedPacketReservation.class));
        invokeAfterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        verify(fixture.registry).release(same(fixture.reservation));
        verify(fixture.registry, never()).confirm(any(RedPacketReservation.class));
    }

    @Test
    void persistedReceiveReturnsRecordedAmountWithoutReservingAgain() {
        Fixture fixture = new Fixture();
        BigDecimal persistedAmount = new BigDecimal("3.21");
        when(fixture.receiveMapper.selectByPacketIdAndReceiverId(RED_PACKET_ID, USER_ID))
                .thenReturn(new RedPacketReceive().setAmount(persistedAmount));

        ReceiveRedPacketResponse response = fixture.service.receiveRedPacket(USER_ID, RED_PACKET_ID);

        assertEquals(persistedAmount, response.getReceivedAmount());
        assertEquals(Integer.valueOf(0), response.getStatus());
        assertNull(response.getMessage());
        verifyNoInteractions(fixture.registry, fixture.redPacketMapper);
    }

    private static Fixture successfulFixture() {
        Fixture fixture = new Fixture();
        fixture.reservation = new RedPacketReservation(RED_PACKET_ID, USER_ID, "token-1", 1_000L);
        when(fixture.receiveMapper.selectByPacketIdAndReceiverId(RED_PACKET_ID, USER_ID)).thenReturn(null);
        when(fixture.registry.reserve(RED_PACKET_ID, USER_ID))
                .thenReturn(ReservationResult.reserved(fixture.reservation));
        when(fixture.redPacketMapper.selectByIdForUpdate(RED_PACKET_ID)).thenReturn(new RedPacket()
                .setRedPacketId(RED_PACKET_ID)
                .setRedPacketType(1)
                .setTotalAmount(new BigDecimal("10.00"))
                .setTotalCount(2)
                .setRemainingAmount(new BigDecimal("10.00"))
                .setRemainingCount(2)
                .setStatus(RedPacketStatus.UNCLAIMED.getStatus()));
        when(fixture.redPacketMapper.updateById(any(RedPacket.class))).thenReturn(1);
        when(fixture.receiveMapper.insert(any(RedPacketReceive.class))).thenReturn(1);
        when(fixture.userBalanceMapper.selectById(USER_ID))
                .thenReturn(new UserBalance().setUserId(USER_ID).setBalance(new BigDecimal("20.00")));
        when(fixture.userBalanceMapper.updateById(any(UserBalance.class))).thenReturn(1);
        when(fixture.balanceLogMapper.insert(any())).thenReturn(1);
        return fixture;
    }

    private static void invokeAfterCommit() {
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        for (TransactionSynchronization synchronization : synchronizations) {
            synchronization.afterCommit();
        }
    }

    private static void invokeAfterCompletion(int status) {
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        for (TransactionSynchronization synchronization : synchronizations) {
            synchronization.afterCompletion(status);
        }
    }

    private static final class Fixture {
        private final UserBalanceMapper userBalanceMapper = mock(UserBalanceMapper.class);
        private final BalanceLogMapper balanceLogMapper = mock(BalanceLogMapper.class);
        private final RedPacketReceiveMapper receiveMapper = mock(RedPacketReceiveMapper.class);
        private final RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        private final GetRedPacketService getRedPacketService = mock(GetRedPacketService.class);
        private final RedPacketReservationRegistry registry = mock(RedPacketReservationRegistry.class);
        private final RedPacketMapper redPacketMapper = mock(RedPacketMapper.class);
        private final RedPacketReceiveService service = new RedPacketReceiveService(userBalanceMapper,
                balanceLogMapper, receiveMapper, redisTemplate, getRedPacketService, registry);
        private RedPacketReservation reservation;

        private Fixture() {
            ReflectionTestUtils.setField(service, "baseMapper", redPacketMapper);
        }
    }
}
