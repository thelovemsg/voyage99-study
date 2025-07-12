package kr.hhplus.be.server.concert.service;

import kr.hhplus.be.server.common.exceptions.ParameterNotValidException;
import kr.hhplus.be.server.common.messages.MessageCode;
import kr.hhplus.be.server.common.utils.IdUtils;
import kr.hhplus.be.server.concert.domain.ConcertEntity;
import kr.hhplus.be.server.concert.domain.ConcertScheduleEntity;
import kr.hhplus.be.server.concert.domain.VenueEntity;
import kr.hhplus.be.server.concert.controller.dto.ConcertScheduleCreateDto;
import kr.hhplus.be.server.concert.repository.ConcertJpaRepository;
import kr.hhplus.be.server.concert.repository.ConcertScheduleJpaRepository;
import kr.hhplus.be.server.concert.repository.VenueJpaRepository;
import kr.hhplus.be.server.concert.service.validator.ConcertScheduleValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConcertScheduleServiceTest {

    @Mock
    private ConcertScheduleJpaRepository concertScheduleJpaRepository;

    @Mock
    private ConcertJpaRepository concertJpaRepository;

    @Mock
    private ConcertScheduleValidator concertScheduleValidator;

    @Mock
    private VenueJpaRepository venueJpaRepository;

    @InjectMocks
    private ConcertScheduleService concertScheduleService;

    private ConcertEntity concert;
    private VenueEntity venue;
    private ConcertScheduleCreateDto.Request scheduleCreateRequestDto;

    private Long concertId;
    private Long venueId;
    private LocalDate now;

    @BeforeEach
    void setUp() {
        this.concertId = IdUtils.getNewId();
        this.venueId = IdUtils.getNewId();

        now = LocalDate.now();
        // 테스트용 공연 데이터 준비
        concert = ConcertEntity.builder()
                .concertId(this.concertId)
                .concertName("나훈아 스페셜 쇼")
                .artistName("나훈아")
                .description("디즈니 뮤지컬")
                .build();

        // 테스트용 공연장 데이터 준비
        venue = VenueEntity.builder()
                .venueId(this.venueId)
                .venueName("venueName")
                .venueAddress("venueAddress")
                .venueCallNumber("venueCallNumber")
                .totalCapacity(100)
                .description("description")
                .build();

        // 테스트용 생성 DTO 준비
        scheduleCreateRequestDto = new ConcertScheduleCreateDto.Request();
        scheduleCreateRequestDto.setConcertId(this.concertId);
        scheduleCreateRequestDto.setVenueId(this.venueId);
        scheduleCreateRequestDto.setConcertDate(this.now);
        scheduleCreateRequestDto.setConcertStartTime(LocalTime.of(1,1,1));
        scheduleCreateRequestDto.setConcertEndTime(LocalTime.of(2,2,2));
        scheduleCreateRequestDto.setTotalSeatsNumber(100);
    }

    @Test
    @DisplayName("콘서트 일정 생성")
    void createConcertSchedule() {
        //given
        Long concertScheduleId = IdUtils.getNewId();

        ConcertScheduleEntity expectedScheduleEntity = ConcertScheduleEntity
                .builder()
                .concertScheduleId(concertScheduleId)
                .totalSeatsNumber(100)
                .concertId(concertId)
                .venueId(venueId)
                .concertDate(now)
                .concertStartTime(LocalTime.of(1,1,1))
                .concertEndTime(LocalTime.of(2,2,2))
                .build();

        doNothing().when(concertScheduleValidator).validate(any());
        // WARNING! => validator 안에 이미 concertRepository, venueRepository를
        // 선언햇는데, mock 을 validator 를 등록하고 또 concertRepository, venueRepository를 mock으로
        // 또 선언하면 꼬여서 에러가 남.
//        when(concertRepository.findById(concertId)).thenReturn(Optional.of(concert));
//        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));
        when(concertScheduleJpaRepository.save(any(ConcertScheduleEntity.class))).thenReturn(expectedScheduleEntity);

        //when
        ConcertScheduleCreateDto.Response result = concertScheduleService.createConcertSchedule(scheduleCreateRequestDto);

        //then
        assertThat(result).isNotNull();
        assertThat(result.getConcertScheduleId()).isEqualTo(concertScheduleId);

//        verify(concertRepository).findById(concertId);
//        verify(venueRepository).findById(venueId);
        verify(concertScheduleJpaRepository).save(any(ConcertScheduleEntity.class));
        verify(concertScheduleJpaRepository, times(1)).save(any(ConcertScheduleEntity.class));
        verify(concertScheduleValidator, times(1)).validate(scheduleCreateRequestDto);

    }

    @Test
    @DisplayName("콘서트 일정 생성 에러 - 공연 시간 최소 30분 이상이어야 함.")
    void createConcertScheduleError() {
        //given
        doThrow(new ParameterNotValidException(MessageCode.CONCERT_SCHEDULE_PLAY_TIME_NOT_PROPER_ERROR))
                .when(concertScheduleValidator).validate(any(ConcertScheduleCreateDto.Request.class));

        //when
        assertThatThrownBy(() -> concertScheduleService.createConcertSchedule(scheduleCreateRequestDto))
                .isInstanceOf(ParameterNotValidException.class);

        //then
        verify(concertScheduleValidator, times(1)).validate(scheduleCreateRequestDto);
        verify(concertScheduleJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("콘서트 일정 생성 에러 - 공연장 없음")
    void createScheduleError_no_venue() {
        //given
        doThrow(new ParameterNotValidException(MessageCode.VENUE_NOT_FOUND, venueId))
                .when(concertScheduleValidator).validate(any(ConcertScheduleCreateDto.Request.class));

        //when
        assertThatThrownBy(() -> concertScheduleService.createConcertSchedule(scheduleCreateRequestDto))
                .isInstanceOf(ParameterNotValidException.class);

        //then
        verify(concertScheduleValidator, times(1)).validate(scheduleCreateRequestDto);
        verify(concertScheduleJpaRepository, never()).save(any());
    }

}
