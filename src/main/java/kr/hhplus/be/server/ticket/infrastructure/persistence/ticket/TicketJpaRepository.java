package kr.hhplus.be.server.ticket.infrastructure.persistence.ticket;

import jakarta.persistence.LockModeType;
import kr.hhplus.be.server.ticket.domain.enums.TicketStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TicketJpaRepository extends JpaRepository<TicketEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TicketEntity t WHERE t.ticketId = :ticketId")
    Optional<TicketEntity> findByIdWithLock(Long ticketId);
    List<TicketEntity> findByUserId(Long userId);
    Optional<TicketEntity> findByTicketNo(String ticketNo);
    boolean existsBySeatIdAndTicketStatusEnum(Long seatId, TicketStatusEnum status);
    List<TicketEntity> findListByConcertScheduleId(Long concertScheduleId);
    @Modifying
    @Query("""
        UPDATE TicketEntity t 
        SET t.reservedBy = :userId,
            t.reservedUntil = :expireTime,
            t.ticketStatusEnum = 'RESERVED'
        WHERE t.ticketId = :ticketId 
          AND (t.ticketStatusEnum = 'AVAILABLE' 
               OR (t.reservedUntil < CURRENT_TIMESTAMP AND t.ticketStatusEnum = 'RESERVED'))
        """)
    int reserveTicketAtomically(@Param("ticketId") Long ticketId,
                                @Param("userId") Long userId,
                                @Param("expireTime") LocalDateTime expireTime);

    @Modifying
    @Query("UPDATE TicketEntity t SET t.ticketStatusEnum = :status, t.userId = :userId, t.purchaseDateTime = :purchaseDateTime " +
            "WHERE t.ticketId = :ticketId AND t.ticketStatusEnum = 'RESERVED' AND t.userId = :originalUserId")
    int updateWithOptimisticLock(
            @Param("ticketId") Long ticketId,
            @Param("status") TicketStatusEnum status,
            @Param("userId") Long userId,
            @Param("originalUserId") Long originalUserId,
            @Param("purchaseDateTime") LocalDateTime purchaseDateTime
    );
}
