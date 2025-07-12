package kr.hhplus.be.server.concert.controller;

import io.hypersistence.tsid.TSID;
import kr.hhplus.be.server.common.exceptions.NotFoundException;
import kr.hhplus.be.server.common.messages.MessageCode;
import kr.hhplus.be.server.concert.controller.dto.ConcertCreateDto;
import kr.hhplus.be.server.concert.controller.dto.ConcertInfoDto;
import kr.hhplus.be.server.concert.repository.ConcertJpaRepository;
import kr.hhplus.be.server.concert.service.ConcertService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConcertController.class)
class ConcertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private ConcertJpaRepository repository;

    // @MockBean이 deprecated 되었다고 한다!
    @MockitoBean
    private ConcertService concertService;  // Service만 모킹

    @Test
    @DisplayName("콘서트 조회 - 단건")
    void getConcert() throws Exception {
        Long concertId = TSID.fast().toLong();
        String concertName = "concert-name";
        String artist = "artist";
        String description = "description";

        ConcertInfoDto.Response response = ConcertInfoDto.Response.builder()
                .concertId(concertId)
                .concertName(concertName)
                .artistName(artist)
                .description(description)
                .build();

        Mockito.when(concertService.findById(concertId)).thenReturn(response);

        mockMvc.perform(get("/concerts/{id}", concertId))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(jsonPath("$.concertId").value(concertId))
                .andExpect(jsonPath("$.artistName").value(artist))
                .andExpect(jsonPath("$.description").value(description));

        verify(concertService).findById(concertId);
    }

    @Test
    @DisplayName("콘서트 조회 에러 - 미존재")
    void noConcert() throws Exception {
        //given
        Long concertId = TSID.fast().toLong();

        //when
        Mockito.when(concertService.findById(concertId)).thenThrow(new NotFoundException(MessageCode.CONCERT_NOT_FOUND, concertId));

        //then
        mockMvc.perform(get("/concerts/{id}", concertId)).andExpect(status().is4xxClientError());  // 500 에러 확인
        verify(concertService).findById(concertId);
    }

    @Test
    @DisplayName("콘서트 생성")
    void saveConcert() throws Exception {

        // Given
        Long concertId = TSID.fast().toLong();
        ObjectMapper objectMapper = new ObjectMapper();

        ConcertCreateDto.Request request = new ConcertCreateDto.Request();
        request.setConcertName("concert-name");
        request.setArtistName("artist");
        request.setDescription("description");

        ConcertCreateDto.Response response = new ConcertCreateDto.Response(concertId);


       /*
       *  WARNING!
       *
       *  안되던 코드 => any를 사용해서 처리함.
       *  내가 여기서 역직렬화 해서 만든 request 와 실제로 코드가 비교할 내가 세팅해준 request 가 달라서 생긴 오류
       *
       *   ConcertCreateDto.Request request = new ConcertCreateDto.Request();
            request.setConcertName(concertName);
            request.setArtistName(artist);
            request.setDescription(description);

            ConcertCreateDto.Response response = new ConcertCreateDto.Response(concertId);

            Mockito.when(concertService.createConcert(request)).thenReturn(response);
       * */

        when(concertService.createConcert(any(ConcertCreateDto.Request.class)))
                .thenReturn(response);

        // When & Then
        String responseBody = mockMvc.perform(post("/concerts")  // ✅ 이제 매칭됨!
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        System.out.println("Response: " + responseBody);
    }

}