package kr.hhplus.be.server.ticket.application.service.redis;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.concert.event.ConcertSoldOutEvent;
import kr.hhplus.be.server.common.exceptions.NotFoundException;
import kr.hhplus.be.server.common.exceptions.TicketPurchaseException;
import kr.hhplus.be.server.common.messages.MessageCode;
import kr.hhplus.be.server.common.redis.RedisCacheTemplate;
import kr.hhplus.be.server.common.redis.RedisDistributedLockTemplate;
import kr.hhplus.be.server.common.redis.RedisKeyUtils;
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
import org.springframework.stereotype.Service;

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
    private final ApplicationEventPublisher eventPublisher;

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
                        UserEntity userEntity = ticketDomainService.validateUserHasEnoughPoint(userId, request.getUseAmount());
                        ticketDomainService.useUserPoint(userEntity, request.getUseAmount());

                        // 5. 구매처리
                        ticket.completePurchase(userId); // 상태를 PAID로 변경

                        // 6. 티켓 구매 정보 전송용 이벤트 호출
                        eventPublisher.publishEvent(ConcertTicketPurchaseEvent.fromTicketEntity(ticket));

                        // 7. 매진 체크 및 이벤트
                        if (remaining == 0) {
                            eventPublisher.publishEvent(new ConcertSoldOutEvent(concertScheduleId, ticket.getConcertInfo(), LocalDateTime.now()));
                        }

                        return PurchaseTicketCommandDto.Response.builder()
                                .ticketId(ticketId)
                                .isSuccess(Boolean.TRUE)
                                .build();
                    } catch (Exception e) {
                        // ⭐ 예외 발생 시 좌석 수 복구!
                        log.warn("티켓 구매 중 예외 발생, 좌석 수 복구: ticketId={}, scheduleId={}", ticketId, concertScheduleId, e);
                        redisCacheTemplate.incrementRemaining(remainingKey);
                        throw e; // 예외 재발생
                    }
                },
                () -> new TicketPurchaseException(MessageCode.TICKET_PURCHASE_ERROR, ticketId)
        );
    }

}