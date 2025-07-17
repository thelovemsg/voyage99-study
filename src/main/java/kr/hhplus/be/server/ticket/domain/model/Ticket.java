package kr.hhplus.be.server.ticket.domain.model;

import kr.hhplus.be.server.common.exceptions.ParameterNotValidException;
import kr.hhplus.be.server.common.messages.MessageCode;
import kr.hhplus.be.server.ticket.domain.enums.TicketStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Ticket {

    private Long ticketId;
    private Long userId;
    private Long seatId;
    private Long concertScheduleId;
    private String ticketNo;
    private String concertInfo;
    private String seatInfo;
    private TicketStatusEnum ticketStatus;
    private LocalDateTime purchaseDateTime;
    private BigDecimal totalAmount;
    private LocalDateTime cancelledAt;

    // 예약 관련 필드 (임시 예약용)
    private LocalDateTime reservedUntil;
    private Long reservedBy;


    public void reserve(Long userId) {
        if (!isReservable()) {
            throw new ParameterNotValidException(MessageCode.TICKET_RESERVATION_NOT_AVAILABLE);
        }

        if (!isReservationExpired() && isReservedByOther(userId)) {
            throw new ParameterNotValidException(MessageCode.TICKET_ALREADY_RESERVED_ERROR);
        }

        this.reservedBy = userId;
        this.reservedUntil = LocalDateTime.now().plusMinutes(5);
        this.ticketStatus = TicketStatusEnum.RESERVED;
    }

    public boolean isReservedByOther(Long userId) {
        return reservedBy != null && !reservedBy.equals(userId);
    }

    public boolean isReservationExpired() {
        if(reservedBy == null) return Boolean.TRUE;
        return reservedUntil != null && LocalDateTime.now().isAfter(reservedUntil);
    }

    public static Ticket createUnusedTicket(Long seatId, Long concertScheduleId, String ticketNo, BigDecimal totalAmount) {
        return new Ticket(
                null, // ID는 저장 시 생성
                null,
                seatId,
                concertScheduleId,
                ticketNo,
                null, // concertInfo는 나중에 설정
                null, // seatInfo는 나중에 설정
                TicketStatusEnum.AVAILABLE,
                null, // purchaseDateTime은 구매 완료 시 설정
                totalAmount,
                null,
                null, // reservedUntil
                null  // reservedBy
        );
    }

    public void completePurchase(Long userId) {
        validatePurchaseState(userId);
        this.ticketStatus = TicketStatusEnum.PAID;
        this.purchaseDateTime = LocalDateTime.now();
        this.userId = userId;
    }

    // 검증 로직
    private void validatePurchaseState(Long userId) {
        if (this.ticketStatus != TicketStatusEnum.RESERVED) {
            throw new IllegalStateException("예약된 티켓만 구매할 수 있습니다.");
        }

        if (!userId.equals(this.reservedBy)) {
            throw new IllegalStateException("예약한 사용자만 구매할 수 있습니다.");
        }

        if (isReservationExpired()) {
            throw new IllegalStateException("예약이 만료되었습니다.");
        }
    }

    private boolean isReservable() {
        return this.ticketStatus == TicketStatusEnum.AVAILABLE ||
                (this.ticketStatus == TicketStatusEnum.RESERVED && isReservationExpired());
    }

    public void cancel() {
        if (this.ticketStatus != TicketStatusEnum.PAID) {
            throw new IllegalStateException("결제 완료된 티켓만 취소할 수 있습니다.");
        }

        // 🆕 실제 취소 처리 - 재판매 가능하도록
        this.ticketStatus = TicketStatusEnum.AVAILABLE;
        this.cancelledAt = LocalDateTime.now();

        // 🆕 재판매를 위한 초기화
        this.userId = null;
        this.purchaseDateTime = null;
        clearReservation();
    }

    private void clearReservation() {
        this.reservedBy = null;
        this.reservedUntil = null;
        this.ticketStatus = TicketStatusEnum.AVAILABLE;
    }

}