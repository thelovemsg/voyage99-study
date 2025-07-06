package kr.hhplus.be.server.ticket.application.service;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.common.exceptions.NotFoundException;
import kr.hhplus.be.server.common.exceptions.TicketPurchaseException;
import kr.hhplus.be.server.common.messages.MessageCode;
import kr.hhplus.be.server.ticket.application.port.in.PurchaseTicketPessimisticLockUseCase;
import kr.hhplus.be.server.ticket.application.port.in.dto.PurchaseTicketCommandDto;
import kr.hhplus.be.server.ticket.domain.model.Ticket;
import kr.hhplus.be.server.ticket.domain.repository.TicketRepository;
import kr.hhplus.be.server.ticket.domain.service.TicketDomainService;
import kr.hhplus.be.server.user.domain.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseTicketPessimisticLockServiceImpl implements PurchaseTicketPessimisticLockUseCase {

    private final TicketRepository ticketRepository;
    private final TicketDomainService ticketDomainService;

    @Override
    @Transactional
    public PurchaseTicketCommandDto.Response purchaseWithPessimisticLock(PurchaseTicketCommandDto.Request request) {
        Long userId = request.getUserId();
        Long concertScheduleId = request.getConcertScheduleId();
        Long ticketId = request.getTicketId();

        try {
            // 3. 티켓 조회 (락 사용으로 동시성 제어)
            Ticket ticket = ticketRepository.findByIdWithLock(ticketId)
                    .orElseThrow(() -> new NotFoundException(MessageCode.TICKET_NOT_FOUND, ticketId));

            // 4. 도메인 검증들
            ticketDomainService.validateConcertScheduleAvailable(concertScheduleId);
            ticketDomainService.validateTicketCanBeReserved(ticket, userId);

            // 5. 비즈니스 로직 실행
            UserEntity userEntity = ticketDomainService.validateUserHasEnoughPoint(userId, request.getUseAmount());
            ticketDomainService.useUserPoint(userEntity, request.getUseAmount());

            // 6. 저장
            ticket.completePurchase(userId); // 상태를 PAID로 변경
            ticketRepository.save(ticket);

            return PurchaseTicketCommandDto.Response.builder()
                    .ticketId(ticketId)
                    .isSuccess(Boolean.TRUE)
                    .build();
        } catch (Exception e) {
            log.error("purchase ticket exception", e);
            throw new TicketPurchaseException(MessageCode.TICKET_PURCHASE_ERROR, ticketId);
        }
    }
}
