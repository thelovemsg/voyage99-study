package kr.hhplus.be.server.ticket.application.service;

import kr.hhplus.be.server.common.exceptions.ParameterNotValidException;
import kr.hhplus.be.server.common.messages.MessageCode;
import kr.hhplus.be.server.common.utils.IdUtils;
import kr.hhplus.be.server.ticket.application.port.in.dto.GetTicketCommandDto;
import kr.hhplus.be.server.ticket.domain.enums.TicketStatusEnum;
import kr.hhplus.be.server.ticket.domain.model.Ticket;
import kr.hhplus.be.server.ticket.infrastructure.persistence.ticket.TicketEntity;
import kr.hhplus.be.server.ticket.infrastructure.persistence.ticket.TicketMapper;
import kr.hhplus.be.server.ticket.infrastructure.persistence.ticket.TicketRepositoryImpl;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class GetTicketServiceImplTest {

    @Mock
    TicketRepositoryImpl ticketRepositoryImpl;

    @InjectMocks
    GetTicketServiceImpl service;

    private Long ticketId;
    private Long userId;
    private Long seatId;
    private Long concertScheduleId;
    private String ticketNo;
    private String concertInfo;
    private String seatInfo;
    private BigDecimal totalAmount;
    private LocalDateTime purchaseDateTime;

    private GetTicketCommandDto.Response response;
    private TicketEntity mockTicket;

    @BeforeEach
    void setup() {

        this.ticketId = IdUtils.getNewId();
        this.userId = IdUtils.getNewId();
        this.seatId = IdUtils.getNewId();
        this.ticketNo = "test1111";
        this.concertScheduleId = IdUtils.getNewId();
        this.concertInfo = "CONERT-20250608-1111";
        this.seatInfo = "좌석-1열-2행-STD석";
        this.totalAmount = new BigDecimal("10000");
        this.purchaseDateTime = LocalDateTime.now();

        this.response = new GetTicketCommandDto.Response();
        this.response.setTicketId(this.ticketId);
        this.response.setTicketNo(ticketNo);
        this.response.setConcertScheduleId(concertScheduleId);
        this.response.setConcertInfo(concertInfo);
        this.response.setSeatInfo(seatInfo);
        this.response.setTicketStatus(TicketStatusEnum.RESERVED);
        this.response.setPurchaseDateTime(purchaseDateTime);
        this.response.setTotalAmount(totalAmount);

        mockTicket = TicketEntity.builder()
                .ticketId(this.ticketId)
                .userId(this.userId)
                .seatId(this.seatId)
                .concertScheduleId(this.concertScheduleId)
                .ticketNo(this.ticketNo)
                .concertInfo(this.concertInfo)
                .seatInfo(this.seatInfo)
                .ticketStatusEnum(TicketStatusEnum.AVAILABLE)
                .purchaseDateTime(LocalDateTime.now())
                .totalAmount(this.totalAmount)
                .reservedUntil(purchaseDateTime.plusMinutes(5))
                .reservedBy(this.userId)
                .build();
    }

    @Test
    @DisplayName("티켓 조회 - 성공")
    void getTicketInfo() {
        //given
        Long ticketId = this.ticketId;

        Ticket ticketDomain = TicketMapper.toDomain(this.mockTicket);

        //when
        Mockito.when(ticketRepositoryImpl.findById(this.ticketId)).thenReturn(Optional.of(ticketDomain));

        //then
        GetTicketCommandDto.Response ticket = service.getTicket(ticketId);

        Assertions.assertThat(ticket.getTicketId()).isEqualTo(ticketId);
    }

    @Test
    @DisplayName("티켓 조회 - 실패")
    void failGettingTicketInfo() {
        //given
        Long ticketId = IdUtils.getNewId();

        Ticket ticketDomain = TicketMapper.toDomain(this.mockTicket);

        //when
        Mockito.when(ticketRepositoryImpl.findById(this.ticketId)).thenThrow(new ParameterNotValidException(MessageCode.TICKET_NOT_FOUND));

        //then
        assertThatThrownBy(() -> service.getTicket(ticketId))
                .isInstanceOf(RuntimeException.class);

        Mockito.verify(ticketRepositoryImpl).findById(ticketId);

    }

}