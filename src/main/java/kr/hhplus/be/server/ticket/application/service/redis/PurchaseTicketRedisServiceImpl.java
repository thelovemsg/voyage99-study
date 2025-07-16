package kr.hhplus.be.server.ticket.application.service.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import kr.hhplus.be.server.common.exceptions.NotFoundException;
import kr.hhplus.be.server.common.exceptions.TicketPurchaseException;
import kr.hhplus.be.server.common.messages.MessageCode;
import kr.hhplus.be.server.common.redis.RedisCacheTemplate;
import kr.hhplus.be.server.common.redis.RedisDistributedLockTemplate;
import kr.hhplus.be.server.common.redis.RedisKeyUtils;
import kr.hhplus.be.server.common.retry.model.EventTypeEnum;
import kr.hhplus.be.server.common.retry.model.OutboxEventEntity;
import kr.hhplus.be.server.common.retry.repository.OutboxEventRepository;
import kr.hhplus.be.server.concert.event.ConcertSoldOutEvent;
import kr.hhplus.be.server.concert.event.ConcertTicketPurchaseEvent;
import kr.hhplus.be.server.ticket.application.port.in.PurchaseTicketRedisUseCase;
import kr.hhplus.be.server.ticket.application.port.in.dto.PurchaseTicketCommandDto;
import kr.hhplus.be.server.ticket.domain.model.Ticket;
import kr.hhplus.be.server.ticket.domain.repository.TicketRepository;
import kr.hhplus.be.server.ticket.domain.service.TicketDomainService;
import kr.hhplus.be.server.user.domain.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 좌석 정보도 현재 관리하려고 했는데,
 * 시간이 없는 관계로 핵심만 작성 => ticket
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseTicketRedisServiceImpl implements PurchaseTicketRedisUseCase {

    private final RedisDistributedLockTemplate lockTemplate;
    private final TicketDomainService ticketDomainService;
    private final TicketRepository ticketRepository;
    private final RedisCacheTemplate redisCacheTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxEventRepository;

    @Override
    @Transactional
    public PurchaseTicketCommandDto.Response purchase(PurchaseTicketCommandDto.Request request) {
        Long userId = request.getUserId();
        Long concertScheduleId = request.getConcertScheduleId();
        Long ticketId = request.getTicketId();

        //이중락 -> ticket을 구매하는 경우,
        String lockKey = RedisKeyUtils.getTicketPurchaseLockKey(ticketId);
        String remainingKey = RedisKeyUtils.getConcertScheduleRemainingKey(concertScheduleId);

        return lockTemplate.executeWithLock(
                lockKey,
                () -> {
                    // 복구를 위한 상태 추적
                    Boolean seatReserved = Boolean.FALSE;
                    Boolean pointDeducted = Boolean.FALSE;
                    UserEntity userEntity = null;

                    Ticket ticket = ticketRepository.findById(ticketId)
                            .orElseThrow(() -> new NotFoundException(MessageCode.TICKET_NOT_FOUND, ticketId));

                    // 2. Redis 잔여 좌석 차감 (원자적 연산)
                    Long remaining = redisCacheTemplate.decrement(remainingKey);
                    if(remaining == null) throw new IllegalArgumentException("에러");

                    if (remaining < 0) {
                        // 좌석 부족 시 롤백
                        redisCacheTemplate.incrementRemaining(remainingKey);
                        throw new TicketPurchaseException(MessageCode.TICKET_RESERVATION_NOT_AVAILABLE, concertScheduleId);
                    }
                    try {
                        // 3. 도메인 검증들
                        ticketDomainService.validateConcertScheduleAvailable(concertScheduleId);
                        ticketDomainService.validateTicketCanBeReserved(ticket, userId);

                        // 4. 비즈니스 로직 실행
                        userEntity = ticketDomainService.validateUserHasEnoughPoint(userId, request.getUseAmount());
                        seatReserved = Boolean.TRUE;

                        ticketDomainService.useUserPoint(userEntity, request.getUseAmount());
                        pointDeducted = Boolean.TRUE;

                        // 5. 구매처리
                        ticket.completePurchase(userId); // 상태를 PAID로 변경

                        String soldOutKey = "concert:soldout:" + concertScheduleId;
                        Boolean isFirst = redisTemplate.opsForValue().setIfAbsent(soldOutKey, "true", Duration.ofHours(24));

                        // 6. 티켓 구매 정보 전송용 이벤트 호출
                        saveEventsToOutbox(ticket, concertScheduleId, isFirst);

                        /**
                        eventPublisher.publishEvent(ConcertTicketPurchaseEvent.fromTicketEntity(ticket));

                        // 7. 매진 체크 및 이벤트
                        if (Boolean.TRUE.equals(isFirst)) {
                            eventPublisher.publishEvent(new ConcertSoldOutEvent(concertScheduleId, ticket.getConcertInfo(), LocalDateTime.now()));
                        }
                        */

                        return PurchaseTicketCommandDto.Response.builder()
                                .ticketId(ticketId)
                                .isSuccess(Boolean.TRUE)
                                .build();
                    } catch (Exception e) {
                        // 예외 발생 시 좌석 수 복구!
                        log.warn("티켓 구매 중 예외 발생, 좌석 수 복구: ticketId={}, scheduleId={}", ticketId, concertScheduleId, e);
                        redisCacheTemplate.incrementRemaining(remainingKey);
                        compensateTransaction(remainingKey, userEntity, request.getUseAmount(), seatReserved, pointDeducted);
                        throw e; // 예외 재발생
                    }
                },
                () -> new TicketPurchaseException(MessageCode.TICKET_PURCHASE_ERROR, ticketId)
        );
    }

    private void saveEventsToOutbox(Ticket ticket, Long concertScheduleId, Boolean isFirst) {
        try {
            // 1. 티켓 구매 이벤트 → Outbox 저장
            ConcertTicketPurchaseEvent purchaseEvent = ConcertTicketPurchaseEvent.fromTicketEntity(ticket);

            OutboxEventEntity ticketEvent = OutboxEventEntity.builder()
                    .aggregateId(ticket.getTicketId().toString())
                    .eventType(EventTypeEnum.TICKET_PURCHASED)  // Enum 사용
                    .payload(objectMapper.writeValueAsString(purchaseEvent))
                    .build();

            outboxEventRepository.save(ticketEvent);

            // 2. 매진 이벤트 → Outbox 저장 (매진인 경우에만)
            if (Boolean.TRUE.equals(isFirst)) {
                ConcertSoldOutEvent soldOutEvent = new ConcertSoldOutEvent(
                        concertScheduleId,
                        ticket.getConcertInfo(),
                        LocalDateTime.now()
                );

                OutboxEventEntity soldOutOutboxEvent = OutboxEventEntity.builder()
                        .aggregateId(concertScheduleId.toString())
                        .eventType(EventTypeEnum.CONCERT_SOLD_OUT)  // Enum 사용
                        .payload(objectMapper.writeValueAsString(soldOutEvent))
                        .build();

                outboxEventRepository.save(soldOutOutboxEvent);
            }

            log.info("이벤트 Outbox 저장 완료: ticketId={}, concertScheduleId={}",
                    ticket.getTicketId(), concertScheduleId);

        } catch (Exception e) {
            log.error("Outbox 이벤트 저장 실패", e);
            throw new RuntimeException("이벤트 저장 실패", e); // 트랜잭션 롤백
        }
    }

    private void compensateTransaction(String remainingKey, UserEntity userEntity, BigDecimal useAmount,
                                       boolean seatReserved, boolean pointDeducted) {
        try {
            // 1. 포인트 복구 (먼저 실행 - DB 트랜잭션)
            if (pointDeducted && userEntity != null) {
                ticketDomainService.refundUserPoint(userEntity, useAmount);
                log.info("포인트 복구 완료: userId={}, amount={}", userEntity.getUserId(), useAmount);
            }

            // 2. 좌석 복구 (나중에 실행 - Redis)
            if (seatReserved) {
                redisCacheTemplate.incrementRemaining(remainingKey);
                log.info("좌석 복구 완료: remainingKey={}", remainingKey);
            }

        } catch (Exception compensationException) {
            log.error("보상 트랜잭션 실행 중 오류 발생 - 수동 처리 필요", compensationException);
            //에러 발생시 데이터 별도의 db 에 에러용 log 로 적재해야함.
        }
    }

}