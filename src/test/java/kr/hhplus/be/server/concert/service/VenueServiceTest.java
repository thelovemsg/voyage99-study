package kr.hhplus.be.server.concert.service;

import kr.hhplus.be.server.common.utils.IdUtils;
import kr.hhplus.be.server.concert.controller.dto.VenueCreateDto;
import kr.hhplus.be.server.concert.domain.VenueEntity;
import kr.hhplus.be.server.concert.controller.dto.VenueInfoDto;
import kr.hhplus.be.server.concert.repository.VenueJpaRepository;
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
class VenueServiceTest {

    @Mock
    private VenueJpaRepository venueJpaRepository;

    @InjectMocks
    private VenueService venueService;

    private final String VENUE_ID = "venueId";

    @Test
    @DisplayName("공연장 정보 생성")
    void createVenueInfo() {
        //given
        Long id = IdUtils.getNewId();

        String venueName = "venueName";
        String venueCallNumber = "010-1111-2222";
        Integer totalCapacity = 50;
        String description = "description";

        VenueCreateDto.Request request = new VenueCreateDto.Request();
        request.setVenueAddress(venueName);
        request.setVenueCallNumber(venueCallNumber);
        request.setTotalCapacity(totalCapacity);
        request.setDescription(description);

        VenueEntity expectedVenue = VenueEntity
                .builder()
                .venueName(venueName)
                .venueCallNumber(venueCallNumber)
                .totalCapacity(totalCapacity)
                .description(description)
                .build();

        ReflectionTestUtils.setField(expectedVenue, VENUE_ID, id);

        //when
        Mockito.when(venueJpaRepository.save(any(VenueEntity.class))).thenReturn(expectedVenue);
        VenueCreateDto.Response savedVenue = venueService.createVenue(request);

        //then
        Assertions.assertThat(savedVenue).isNotNull();
        Assertions.assertThat(savedVenue.getVenueId()).isEqualTo(id);
    }

    @Test
    @DisplayName("공연장 정보 조회")
    void findVenueInfo() {
        //given
        Long id = IdUtils.getNewId();

        String venueName = "venueName";
        String venueCallNumber = "010-1111-2222";
        String venueAddress = "서울";
        Integer totalCapacity = 50;
        String description = "description";

        VenueEntity expectedVenue = VenueEntity
                .builder()
                .venueName(venueName)
                .venueAddress(venueAddress)
                .venueCallNumber(venueCallNumber)
                .totalCapacity(totalCapacity)
                .description(description)
                .build();

        ReflectionTestUtils.setField(expectedVenue, VENUE_ID, id);

        //when
        Mockito.when(venueJpaRepository.findById(id)).thenReturn(Optional.of(expectedVenue));
        VenueInfoDto.Response foundVenue = venueService.findVenue(id);

        //then
        Assertions.assertThat(foundVenue).isNotNull();
        Assertions.assertThat(foundVenue.getVenueId()).isEqualTo(id);
        Assertions.assertThat(foundVenue.getVenueName()).isEqualTo(venueName);
        Assertions.assertThat(foundVenue.getDescription()).isEqualTo(description);
        Assertions.assertThat(foundVenue.getTotalCapacity()).isEqualTo(totalCapacity);
        Assertions.assertThat(foundVenue.getVenueCallNumber()).isEqualTo(venueCallNumber);
    }

}