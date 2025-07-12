package kr.hhplus.be.server.queue.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.hypersistence.tsid.TSID;
import kr.hhplus.be.server.queue.controller.dto.IssueTokenCommandDto;
import kr.hhplus.be.server.queue.controller.dto.QueueCreateCommandDto;
import kr.hhplus.be.server.queue.controller.dto.TokenValidateCommandDto;
import kr.hhplus.be.server.queue.service.QueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class QueueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QueueService queueService;

    @Autowired
    private ObjectMapper objectMapper;

    private QueueCreateCommandDto.Request createRequest;
    private Long userId;
    private Long concertScheduleId;

    @BeforeEach
    void setup() {
        userId = TSID.fast().toLong();
        concertScheduleId = TSID.fast().toLong();

        createRequest = new QueueCreateCommandDto.Request();
        createRequest.setUserId(userId);
        createRequest.setConcertScheduleId(concertScheduleId);
    }

    @Test
    @DisplayName("큐 생성 - 성공")
    void enterQueue_Success() throws Exception {
        // Given
        Long queueId = TSID.fast().toLong();

        QueueCreateCommandDto.Response response = QueueCreateCommandDto.Response.builder()
                .queueId(queueId)
                .userId(userId)
                .status("WAITING")
                .message("큐가 생성되었습니다.")
                .createdAt(LocalDateTime.now())
                .build();

        when(queueService.enterQueue(any(QueueCreateCommandDto.Request.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/queue/enter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.queueId").value(queueId))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.message").value("큐가 생성되었습니다."));

        verify(queueService).enterQueue(any(QueueCreateCommandDto.Request.class));
    }

    @Test
    @DisplayName("큐 생성 - 이미 존재하는 경우")
    void enterQueue_AlreadyExists() throws Exception {
        // Given
        Long queueId = TSID.fast().toLong();

        QueueCreateCommandDto.Response response = QueueCreateCommandDto.Response.builder()
                .queueId(queueId)
                .userId(userId)
                .status("WAITING")
                .message("이미 큐가 생성되어 있습니다.")
                .createdAt(LocalDateTime.now())
                .build();

        when(queueService.enterQueue(any(QueueCreateCommandDto.Request.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/queue/enter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("이미 큐가 생성되어 있습니다."));

        verify(queueService).enterQueue(any(QueueCreateCommandDto.Request.class));
    }

    @Test
    @DisplayName("토큰 검증 - 성공")
    void validateToken_Valid() throws Exception {
        // Given
        String token = "valid-token-123";
        
        TokenValidateCommandDto.Request request = new TokenValidateCommandDto.Request();
        request.setUserId(userId);
        request.setToken(token);
        request.setConcertScheduleId(concertScheduleId);

        TokenValidateCommandDto.Response response = TokenValidateCommandDto.Response.builder()
                .isValid(true)
                .status("VALID")
                .message("유효한 토큰입니다.")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(queueService.validateToken(any(TokenValidateCommandDto.Request.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/queue/token/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isValid").value(true))
                .andExpect(jsonPath("$.status").value("VALID"))
                .andExpect(jsonPath("$.message").value("유효한 토큰입니다."));

        verify(queueService).validateToken(any(TokenValidateCommandDto.Request.class));
    }

    @Test
    @DisplayName("토큰 검증 - 유효하지 않음")
    void validateToken_Invalid() throws Exception {
        // Given
        String token = "invalid-token-123";
        
        TokenValidateCommandDto.Request request = new TokenValidateCommandDto.Request();
        request.setUserId(userId);
        request.setToken(token);
        request.setConcertScheduleId(concertScheduleId);

        TokenValidateCommandDto.Response response = TokenValidateCommandDto.Response.builder()
                .isValid(false)
                .status("INVALID")
                .message("토큰이 일치하지 않습니다.")
                .build();

        when(queueService.validateToken(any(TokenValidateCommandDto.Request.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/queue/token/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isValid").value(false))
                .andExpect(jsonPath("$.status").value("INVALID"))
                .andExpect(jsonPath("$.message").value("토큰이 일치하지 않습니다."));

        verify(queueService).validateToken(any(TokenValidateCommandDto.Request.class));
    }

    @Test
    @DisplayName("토큰 검증 - 만료됨")
    void validateToken_Expired() throws Exception {
        // Given
        String token = "expired-token-123";
        
        TokenValidateCommandDto.Request request = new TokenValidateCommandDto.Request();
        request.setUserId(userId);
        request.setToken(token);
        request.setConcertScheduleId(concertScheduleId);

        TokenValidateCommandDto.Response response = TokenValidateCommandDto.Response.builder()
                .isValid(false)
                .status("EXPIRED")
                .message("토큰이 만료되었습니다.")
                .build();

        when(queueService.validateToken(any(TokenValidateCommandDto.Request.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/queue/token/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isValid").value(false))
                .andExpect(jsonPath("$.status").value("EXPIRED"))
                .andExpect(jsonPath("$.message").value("토큰이 만료되었습니다."));

        verify(queueService).validateToken(any(TokenValidateCommandDto.Request.class));
    }

    @Test
    @DisplayName("큐 상태 확인")
    void getQueueStatus() throws Exception {
        // Given
        Long concertScheduleId = TSID.fast().toLong();

        // When & Then
        mockMvc.perform(get("/api/queue/status")
                        .param("userId", userId.toString())
                        .param("concertScheduleId", concertScheduleId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string("큐 상태 조회 기능은 추후 구현 예정입니다."));
    }
}
