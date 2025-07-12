package kr.hhplus.be.server.queue.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.common.utils.CryptoUtils;
import kr.hhplus.be.server.common.utils.IdUtils;
import kr.hhplus.be.server.queue.controller.dto.QueueCreateCommandDto;
import kr.hhplus.be.server.queue.enums.QueueStatus;
import kr.hhplus.be.server.queue.repository.QueueRepository;
import kr.hhplus.be.server.queue.service.QueueService;
import kr.hhplus.be.server.ticket.application.port.in.dto.PurchaseTicketCommandDto;
import kr.hhplus.be.server.ticket.application.port.in.dto.ReserveTicketCommandDto;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Queue 동시성 통합 테스트")
class QueueIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QueueRepository queueRepository;

    @Autowired
    private QueueService queueService;

    @MockitoBean
    private ReserveTicketServiceImpl reserveTicketService;

    @MockitoBean
    private PurchaseTicketPessimisticLockServiceImpl purchaseTicketService;

    @Autowired
    private CryptoUtils cryptoUtils;

    @Autowired
    private ObjectMapper objectMapper;

    private static final int TOTAL_USERS = 100;
    private static final int ACTIVE_QUEUE_LIMIT = 10;
    private static final Long CONCERT_SCHEDULE_ID = 1L;


    @BeforeEach
    void setup() {
        // 테스트 데이터 초기화
        queueRepository.deleteAll();
    }

    @Test
    @DisplayName("1명 queue 생성 -> 티켓 예약 -> 구매 완료 처리")
    void testQueueForOne() throws Exception {
        // ******** queue 생성 ********
        log.info("*********************** 1. create queue... *************************");

        //given
        Long userId = IdUtils.getNewId();
        Long concertScheduleId = IdUtils.getNewId();
        Long queueId = IdUtils.getNewId();

        String token = "TOKEN_TEST_VALUE";

        QueueCreateCommandDto.Request request = new QueueCreateCommandDto.Request();
        request.setUserId(userId);
        request.setConcertScheduleId(concertScheduleId);

        QueueCreateCommandDto.Response response = new QueueCreateCommandDto.Response();
        response.setQueueId(queueId);
        response.setUserId(userId);
        response.setStatus(QueueStatus.WAITING.name());
        response.setToken(token);
        response.setMessage("큐가 생성되었습니다.");
        response.setCreatedAt(LocalDateTime.now());

        when(queueService.enterQueue(any(QueueCreateCommandDto.Request.class))).thenReturn(response);

        //when
        mockMvc.perform(post("/api/queue/enter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.queueId").value(queueId))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.message").value("큐가 생성되었습니다."));

        verify(queueService).enterQueue(any(QueueCreateCommandDto.Request.class));

        log.info("*********************** 2. reserve ticket ... *************************");

        //given
        Long ticketId = IdUtils.getNewId();

        ReserveTicketCommandDto.Request reserveRequest = new ReserveTicketCommandDto.Request();
        reserveRequest.setTicketId(ticketId);
        reserveRequest.setUserId(userId);

        ReserveTicketCommandDto.Response reserveResponse = new ReserveTicketCommandDto.Response();
        reserveResponse.setTicketId(ticketId);

        Mockito.when(reserveTicketService.reserve(any())).thenReturn(reserveResponse);

        String contentAsString = mockMvc.perform(MockMvcRequestBuilders.patch("/ticket/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reserveRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        log.info("ticket reserve result :: {}", contentAsString);

        log.info("*********************** 3. purchase ticket ... *************************");
        PurchaseTicketCommandDto.Request ticketRequest = new PurchaseTicketCommandDto.Request();
        ticketRequest.setTicketId(ticketId);
        ticketRequest.setUserId(userId);
        ticketRequest.setUseAmount(new BigDecimal("100000"));

        PurchaseTicketCommandDto.Response tickteResponse = new PurchaseTicketCommandDto.Response();
        tickteResponse.setTicketId(ticketId);
        tickteResponse.setSuccess(Boolean.TRUE);

        Mockito.when(purchaseTicketService.purchaseWithPessimisticLock(any())).thenReturn(tickteResponse);

        String ticketPurchaseResponse = mockMvc.perform(post("/ticket/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ticketRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        log.info("ticket purchase result :: {}", ticketPurchaseResponse);
    }

}