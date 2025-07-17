package kr.hhplus.be.server.ticket.infrastructure.persistence.ticket;

import kr.hhplus.be.server.ticket.domain.enums.TicketStatusEnum;
import kr.hhplus.be.server.ticket.domain.model.Ticket;
import kr.hhplus.be.server.ticket.domain.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TicketRepositoryImpl implements TicketRepository {

    private final TicketJpaRepository jpaRepository;

    @Override
    public Ticket save(Ticket ticket) {
        TicketEntity savedEntity = jpaRepository.save(TicketMapper.toEntity(ticket));
        return TicketMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Ticket> findById(Long ticketId) {
        Optional<TicketEntity> entityOptional = jpaRepository.findById(ticketId);
        return entityOptional.map(TicketMapper::toDomain);
    }

    @Override
    public List<Ticket> findByUserId(Long userId) {
        List<TicketEntity> entities = jpaRepository.findByUserId(userId);
        return entities.stream()
                .map(TicketMapper::toDomain)
                .toList();
    }

    @Override
    public List<Ticket> findByConcertScheduleId(Long concertScheduleId) {
        List<TicketEntity> list = jpaRepository.findListByConcertScheduleId(concertScheduleId);
        return list.stream()
                .map(TicketMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Ticket> findByTicketNo(String ticketNo) {
        Optional<TicketEntity> entityOptional = jpaRepository.findByTicketNo(ticketNo);
        return entityOptional.map(TicketMapper::toDomain);
    }

    @Override
    public boolean existsBySeatIdAndTicketStatus(Long seatId, TicketStatusEnum status) {
        return jpaRepository.existsBySeatIdAndTicketStatusEnum(seatId, status);
    }

    @Override
    public int reserveTicketAtomically(Long ticketId, Long userId, LocalDateTime expireTime) {
        return jpaRepository.reserveTicketAtomically(ticketId, userId, expireTime);
    }

    @Override
    public void deleteAll() {
        jpaRepository.deleteAll();
    }

    @Override
    public void saveAll(List<TicketEntity> ticketEntities) {
        jpaRepository.saveAll(ticketEntities);
    }

    @Override
    public int updateWithOptimisticLock(Ticket ticket) {
        return jpaRepository.updateWithOptimisticLock(ticket.getTicketId(), TicketStatusEnum.PAID, ticket.getUserId(), ticket.getUserId(),  LocalDateTime.now());
    }


    @Override
    public Optional<Ticket> findByIdWithLock(Long ticketId) {
        Optional<TicketEntity> entityOptional = jpaRepository.findByIdWithLock(ticketId);
        return entityOptional.map(TicketMapper::toDomain);
    }

}
