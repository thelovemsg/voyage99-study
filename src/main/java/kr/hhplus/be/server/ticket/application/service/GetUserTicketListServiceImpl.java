package kr.hhplus.be.server.ticket.application.service;

import kr.hhplus.be.server.ticket.application.port.in.dto.GetTicketCommandDto;
import kr.hhplus.be.server.ticket.application.port.in.GetUserTicketListUseCase;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetUserTicketListServiceImpl implements GetUserTicketListUseCase {

    @Override
    public List<GetTicketCommandDto.Response> getUserTickets(Long userId) {
        return null;
    }
}
