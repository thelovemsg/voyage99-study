package kr.hhplus.be.server.ticket.domain.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TicketPricePolicyDomainService {

    // private final TicketPricePolicyDomainRepository

    // TODO : 급한건 동시성과 큐 관리임. 이거는 부가적인거고 나중에 작업하면 확장성을 위해 미리 만들어둠
    public BigDecimal calculatePrice(Long seatId, Long concertScheduleId) {
        return BigDecimal.valueOf(10_000);
    }
}