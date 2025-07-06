package kr.hhplus.be.server.ticket.application.service.redis;

import kr.hhplus.be.server.common.exceptions.NotFoundException;
import kr.hhplus.be.server.common.exceptions.ParameterNotValidException;
import kr.hhplus.be.server.common.messages.MessageCode;
import kr.hhplus.be.server.common.redis.RedisDistributedLockTemplate;
import kr.hhplus.be.server.common.redis.RedisKeyUtils;
import kr.hhplus.be.server.ticket.application.port.in.ReserveTicketUseCase;
import kr.hhplus.be.server.ticket.application.port.in.dto.ReserveTicketCommandDto;
import kr.hhplus.be.server.ticket.domain.model.Ticket;
import kr.hhplus.be.server.ticket.domain.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service("reserveTicketRedisService")
@RequiredArgsConstructor
public class ReserveTicketRedisServiceImpl implements ReserveTicketUseCase {

    private final RedisDistributedLockTemplate lockTemplate;
    private final TicketRepository ticketRepository;

    @Override
    public ReserveTicketCommandDto.Response reserve(ReserveTicketCommandDto.Request request) {
        Long ticketId = request.getTicketId();
        Long userId = request.getUserId();

        String lockKey = RedisKeyUtils.getTicketReverseLockKey(ticketId);

        return lockTemplate.executeWithLock(
                lockKey,
                () -> {
                    Ticket ticket = ticketRepository.findById(ticketId)
                            .orElseThrow(() -> new NotFoundException(MessageCode.TICKET_NOT_FOUND, ticketId));

                    ticket.reserve(userId);
                    ticketRepository.save(ticket);

                    return new ReserveTicketCommandDto.Response(ticketId);
                },
                () -> new ParameterNotValidException(MessageCode.TICKET_ALREADY_RESERVED_ERROR, ticketId)
        );
    }
}
