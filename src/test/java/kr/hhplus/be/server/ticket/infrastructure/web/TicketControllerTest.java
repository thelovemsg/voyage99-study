package kr.hhplus.be.server.ticket.infrastructure.web;

import kr.hhplus.be.server.common.exceptions.ParameterNotValidException;
import kr.hhplus.be.server.common.messages.MessageCode;
import kr.hhplus.be.server.common.utils.IdUtils;
import kr.hhplus.be.server.ticket.application.port.in.dto.CreateTicketCommandDto;
import kr.hhplus.be.server.ticket.application.port.in.dto.PurchaseTicketCommandDto;
import kr.hhplus.be.server.ticket.application.port.in.dto.ReserveTicketCommandDto;
import kr.hhplus.be.server.ticket.application.service.CreateTicketServiceImpl;
import kr.hhplus.be.server.ticket.application.service.GetTicketServiceImpl;
import kr.hhplus.be.server.ticket.application.service.PurchaseTicketPessimisticLockServiceImpl;
import kr.hhplus.be.server.ticket.application.service.ReserveTicketServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetTicketServiceImpl getTicketService;

    @MockitoBean
    private CreateTicketServiceImpl createTicketService;

    @MockitoBean
    private PurchaseTicketPessimisticLockServiceImpl purchaseTicketService;

    @MockitoBean
    private ReserveTicketServiceImpl reserveTicketService;

    private ObjectMapper objectMapper = new ObjectMapper();

    private String tempTicketNo;

    @BeforeEach
    void setup() {
        this.tempTicketNo = "TICKET_NO_TEST_1111";
    }

    // 티켓 생성 - Long ticketId 반환하도록 수정
    private Long createTicket(Long concertScheduleId, Long seatId) throws Exception {
        CreateTicketCommandDto.Request request = new CreateTicketCommandDto.Request();
        request.setConcertScheduleId(concertScheduleId);
        request.setSeatId(seatId);

        Long generatedTicketId = IdUtils.getNewId();
        CreateTicketCommandDto.Response response = new CreateTicketCommandDto.Response();
        response.setTicketId(generatedTicketId);

        Mockito.when(createTicketService.createTicket(any())).thenReturn(response);

        String responseBody = mockMvc.perform(MockMvcRequestBuilders.post("/ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return extractTicketId(responseBody);
    }

    // 예약 - Long ticketId 사용하도록 수정
    private String reserveTicket(Long ticketId, Long userId) throws Exception {
        ReserveTicketCommandDto.Request request = new ReserveTicketCommandDto.Request();
        request.setTicketId(ticketId);
        request.setUserId(userId);

        ReserveTicketCommandDto.Response response = new ReserveTicketCommandDto.Response();
        response.setTicketId(ticketId);

        Mockito.when(reserveTicketService.reserve(any())).thenReturn(response);

        return mockMvc.perform(MockMvcRequestBuilders.patch("/ticket/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    // 구매 메서드 (누락된 부분!)
    private String purchaseTicket(Long ticketId, Long userId) throws Exception {
        PurchaseTicketCommandDto.Request request = new PurchaseTicketCommandDto.Request();
        request.setTicketId(ticketId);
        request.setUserId(userId);
        request.setUseAmount(new BigDecimal("100000"));

        PurchaseTicketCommandDto.Response response = new PurchaseTicketCommandDto.Response();
        response.setTicketId(ticketId);
        response.setSuccess(Boolean.TRUE);

        Mockito.when(purchaseTicketService.purchaseWithPessimisticLock(any())).thenReturn(response);

        return mockMvc.perform(MockMvcRequestBuilders.post("/ticket/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }


    private void reserveTicketShouldFail(Long ticketId, Long userId) throws Exception {
        ReserveTicketCommandDto.Request request = new ReserveTicketCommandDto.Request();
        request.setTicketId(ticketId);
        request.setUserId(userId);

        Mockito.when(reserveTicketService.reserve(any()))
                .thenThrow(new ParameterNotValidException(MessageCode.TICKET_ALREADY_RESERVED_ERROR));

        mockMvc.perform(MockMvcRequestBuilders.patch("/ticket/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    private void purchaseTicketFail(Long ticketId, Long userId) throws Exception {
        PurchaseTicketCommandDto.Request request = new PurchaseTicketCommandDto.Request();
        request.setTicketId(ticketId);
        request.setUserId(userId);
        request.setUseAmount(new BigDecimal("300000"));

        PurchaseTicketCommandDto.Response response = new PurchaseTicketCommandDto.Response();
        response.setTicketId(ticketId);
        response.setSuccess(Boolean.TRUE);

        Mockito.when(purchaseTicketService.purchaseWithPessimisticLock(any()))
                .thenThrow(new ParameterNotValidException(MessageCode.USER_POINT_NOT_ENOUGH));

        mockMvc.perform(MockMvcRequestBuilders.post("/ticket/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());

    }

    @Test
    @DisplayName("단순 티켓 생성 테스트")
    void purchaseTicketTest() throws Exception {
        // Given
        Long concertScheduleId = IdUtils.getNewId();
        Long seatId = IdUtils.getNewId();

        // When
        Long tickedId = createTicket(concertScheduleId, seatId);

        // Then
        assertThat(tickedId).isNotNull();
        log.info("티켓 생성 성공: {}", tickedId);

    }

    /**
     * 시나리오 - 심플 버전
     * 1. 티켓 정보를 생성
     * 2. 사용자가 생성한 티켓을 예약한다.
     * 3. 사용자가 다시 구매를 시도하면 성공!
     */
    @Test
    @DisplayName("티켓 생성 -> 예약 처리 -> 구매 테스트 : 유저 1명")
    void create_reserve_purchase_test() throws Exception {
        // Given
        Long userAId = IdUtils.getNewId();

        Long concertScheduleId = IdUtils.getNewId();
        Long seatId = IdUtils.getNewId();

        // 1. 티켓 생성 (Long ticketId 반환)
        Long ticketId = createTicket(concertScheduleId, seatId);
        log.info("1단계 - 티켓 생성 완료: {}", ticketId);

        // 2. 사용자 A가 예약 (Long ticketId 사용)
        String reserveResponse = reserveTicket(ticketId, userAId);
        log.info("2단계 - 사용자 A 예약 완료: {}", reserveResponse);

        // 3. 사용자 A가 구매 (Long ticketId 사용)
        String purchaseResponse = purchaseTicket(ticketId, userAId);
        log.info("3단계 - 사용자 A 구매 성공: {}", purchaseResponse);

        // 검증
        assertThat(ticketId).isNotNull();
        assertThat(ticketId).isPositive();
    }

    /**
     * 시나리오 - 타인이 예약한 티켓의 예약 시도
     * 1. 티켓 정보를 생성
     * 2. 사용자(A)가 생성한 티켓을 예약한다.
     * 3. 다른 사용자(B)가 사용자(A)와 같은 티켓을 예약하려 하면 에러를 반환.
     * 4. 사용자(A)가 예약한 티켓 구매를 성공하고 종료
     */
    @Test
    @DisplayName("티켓 생성 -> 예약 처리 -> 구매 테스트 + 중도 다른 사용자가 예약 시도")
    void create_reserve_purchase_many() throws Exception {
        // Given
        Long userAId = IdUtils.getNewId();
        Long userBId = IdUtils.getNewId();

        Long concertScheduleId = IdUtils.getNewId();
        Long seatId = IdUtils.getNewId();

        // 1. 티켓 생성
        Long ticketId = createTicket(concertScheduleId, seatId);
        log.info("1단계 - 티켓 생성 완료: {}", ticketId);

        // 2. 사용자 A가 예약
        String reserveResponse = reserveTicket(ticketId, userAId);
        log.info("2단계 - 사용자 A 예약 완료: {}", reserveResponse);

        // 3. 사용자 B가 사용자 A가 예약한 ticket을 예약
        reserveTicketShouldFail(ticketId, userBId);
        log.info("3단계 - 사용자 B 예약 에러 발생!");

        // 4. 사용자 A가 구매 (Long ticketId 사용)
        String purchaseResponse = purchaseTicket(ticketId, userAId);
        log.info("4단계 - 사용자 A 구매 성공: {}", purchaseResponse);

        // 검증
        assertThat(ticketId).isNotNull();
        assertThat(ticketId).isPositive();
    }

    /**
     * 시나리오 - 타인이 예약한 티켓의 예약 시도
     * 1. 티켓 정보를 생성
     * 2. 사용자(A)가 생성한 티켓을 예약한다.
     * 3. 사용자(A)가 예약한 티켓 구매를 하려는데 잔액 부족 에러 반환
     */
    @Test
    @DisplayName("티켓 생성 -> 예약 처리 -> 구매 실패 - 잔액부족")
    void create_reserve_purchase_not_enough_money() throws Exception {
        // Given
        Long userAId = IdUtils.getNewId();

        Long concertScheduleId = IdUtils.getNewId();
        Long seatId = IdUtils.getNewId();

        // 1. 티켓 생성 (Long ticketId 반환)
        Long ticketId = createTicket(concertScheduleId, seatId);
        log.info("1단계 - 티켓 생성 완료: {}", ticketId);

        // 2. 사용자 A가 예약 (Long ticketId 사용)
        String reserveResponse = reserveTicket(ticketId, userAId);
        log.info("2단계 - 사용자 A 예약 완료: {}", reserveResponse);

        // 3. 사용자 A가 구매 (Long ticketId 사용)
        purchaseTicketFail(ticketId, userAId);
    }

    private Long extractTicketId(String response) {
        // JSON에서 ticketId 추출 로직
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            return jsonNode.get("ticketId").asLong();
        } catch (Exception e) {
            throw new RuntimeException("티켓 ID 추출 실패: " + response, e);
        }
    }

}