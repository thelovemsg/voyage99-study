package kr.hhplus.be.server.ticket.application.service;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.ticket.application.port.in.CreateTicketUseCase;
import kr.hhplus.be.server.ticket.application.port.in.dto.CreateTicketCommandDto;
import kr.hhplus.be.server.ticket.domain.model.Ticket;
import kr.hhplus.be.server.ticket.domain.port.ConcertScheduleValidationPort;
import kr.hhplus.be.server.ticket.domain.port.GenerateTicketNoPort;
import kr.hhplus.be.server.ticket.domain.port.SeatSearchPort;
import kr.hhplus.be.server.ticket.domain.service.TicketPricePolicyDomainService;
import kr.hhplus.be.server.ticket.infrastructure.persistence.ticket.TicketRepositoryImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CreateTicketServiceImpl implements CreateTicketUseCase {

    private final TicketRepositoryImpl ticketRepositoryImpl;
    private final TicketPricePolicyDomainService ticketPriceService;
    private final SeatSearchPort seatSearchPort;
    private final ConcertScheduleValidationPort scheduleValidationPort;
    private final GenerateTicketNoPort ticketNoPort;

    @Override
    @Transactional
    public CreateTicketCommandDto.Response createTicket(CreateTicketCommandDto.Request request) {

        Long seatId = request.getSeatId();
        Long concertScheduleId = request.getConcertScheduleId();

        // 좌석 및 공연스케줄 존재 검증
        seatSearchPort.seatExist(seatId);
        scheduleValidationPort.concertScheduleExist(concertScheduleId);

        // 금액, 티켓번호 생성
        BigDecimal totalAmount = ticketPriceService.calculatePrice(seatId, concertScheduleId);
        String newTicketNo = ticketNoPort.generateTicketNumber();
        
        //저장
        Ticket unusedTicket = Ticket.createUnusedTicket(seatId, concertScheduleId, newTicketNo, totalAmount);
        Ticket savedTicket = ticketRepositoryImpl.save(unusedTicket);
        return CreateTicketCommandDto.Response.fromDomain(savedTicket);
    }
}
