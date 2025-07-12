package kr.hhplus.be.server.concert.service;

import io.hypersistence.tsid.TSID;
import kr.hhplus.be.server.common.exceptions.NotFoundException;
import kr.hhplus.be.server.concert.controller.dto.ConcertCreateDto;
import kr.hhplus.be.server.concert.controller.dto.ConcertInfoDto;
import kr.hhplus.be.server.concert.domain.ConcertEntity;
import kr.hhplus.be.server.concert.controller.dto.ConcertUpdateDto;
import kr.hhplus.be.server.concert.repository.ConcertJpaRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class ConcertServiceTest {

    @Mock
    private ConcertJpaRepository concertJpaRepository;

    @InjectMocks
    private ConcertService concertService;

    private final String CONCERT_ID = "concertId";

    @Test
    @DisplayName("콘서트를 입력받고 생성한다")
    public void 콘서트_생성() {
        //given
        Long id = TSID.fast().toLong();

        ConcertCreateDto.Request request = new ConcertCreateDto.Request();
        request.setConcertName("name");
        request.setDescription("description");
        request.setArtistName("tester");

        ConcertEntity expectedConcert = ConcertEntity.builder()
                .concertName("name")
                .description("description")
                .artistName("tester")
                .build();

        ReflectionTestUtils.setField(expectedConcert, CONCERT_ID, id);

        //when
        Mockito.when(concertJpaRepository.save(any(ConcertEntity.class))).thenReturn(expectedConcert);

        ConcertCreateDto.Response savedConcert = concertService.createConcert(request);

        //then
        Assertions.assertThat(savedConcert).isNotNull();
        Assertions.assertThat(savedConcert.getConcertId()).isNotNull();
    }

    @Test
    @DisplayName("콘서트 정보 단건 조회")
    public void 콘서트조회_정상() {
        long concertId = TSID.fast().toLong();

        ConcertEntity foundConcert = ConcertEntity.builder()
                .concertId(concertId)
                .concertName("name")
                .description("description")
                .artistName("tester")
                .build();

        Mockito.when(concertJpaRepository.findById(concertId)).thenReturn(Optional.of(foundConcert));

        ConcertInfoDto.Response concert = concertService.findById(concertId);

        Assertions.assertThat(concert.getConcertId()).isEqualTo(foundConcert.getConcertId());

    }

    @Test
    @DisplayName("콘서트 정보 단건 조회 - 데이터 없음")
    public void 콘서트조회_데이터_미존재() {
        long concertId = TSID.fast().toLong();

        Mockito.when(concertJpaRepository.findById(concertId)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> concertService.findById(concertId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("콘서트 정보를 수정한다.")
    public void 콘서트_정보_수정() {

        //given
        long concertId = TSID.fast().toLong();

        ConcertEntity existingUser = ConcertEntity.builder()
                .concertId(concertId)
                .concertName("name")
                .description("description")
                .artistName("artist")
                .build();

        ConcertUpdateDto.Request request = new ConcertUpdateDto.Request();
        request.setConcertId(concertId);
        request.setConcertName("new concertName");
        request.setDescription("new description");
        request.setArtistName("new artistName");

        //when
        Mockito.when(concertJpaRepository.findById(concertId)).thenReturn(Optional.of(existingUser));
        Long updatedConcertId = concertService.updateConcertInfo(request);

        //then
        Mockito.verify(concertJpaRepository).findById(concertId);

        Assertions.assertThat(updatedConcertId).isEqualTo(concertId);
        Assertions.assertThat(existingUser.getArtistName()).isEqualTo("new artistName");
        Assertions.assertThat(existingUser.getConcertName()).isEqualTo("new concertName");
        Assertions.assertThat(existingUser.getDescription()).isEqualTo("new description");
    }

    @Test
    @DisplayName("콘서트 삭제")
    public void deleteConcert() {
        //given
        long concertId = TSID.fast().toLong();

        ConcertEntity existingUser = ConcertEntity.builder()
                .concertId(concertId)
                .concertName("name")
                .description("description")
                .artistName("artist")
                .build();

        //when
        Mockito.when(concertJpaRepository.findById(concertId)).thenReturn(Optional.of(existingUser));
        concertService.deleteConcertInfo(concertId);

        //then
        Mockito.verify(concertJpaRepository).findById(concertId);
        Mockito.verify(concertJpaRepository).delete(existingUser);
    }

}
