package kr.hhplus.be.server.ticket.application.service;

import kr.hhplus.be.server.ticket.application.port.in.dto.CancelTicketCommandDto;
import kr.hhplus.be.server.ticket.application.port.in.CancelTicketUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CancelTicketServiceImpl implements CancelTicketUseCase {

    @Override
    public CancelTicketCommandDto.Response complete(CancelTicketCommandDto.Request request) {
        return null;
    }
}
