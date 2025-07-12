package kr.hhplus.be.server.ticket.domain.repository;

import kr.hhplus.be.server.ticket.domain.enums.TicketStatusEnum;
import kr.hhplus.be.server.ticket.domain.model.Ticket;
import kr.hhplus.be.server.ticket.infrastructure.persistence.ticket.TicketEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TicketRepository {
    Ticket save(Ticket ticket);
    Optional<Ticket> findById(Long ticketId);
    Optional<Ticket> findByIdWithLock(Long ticketId);
    List<Ticket> findByUserId(Long userId);
    List<Ticket> findByConcertScheduleId(Long concertScheduleId);
    Optional<Ticket> findByTicketNo(String ticketNo);
    boolean existsBySeatIdAndTicketStatus(Long seatId, TicketStatusEnum status);
    int reserveTicketAtomically(Long ticketId, Long userId, LocalDateTime expireTime);
    void deleteAll();
    void saveAll(List<TicketEntity> ticketEntities);
    int updateWithOptimisticLock(Ticket ticket);
}
