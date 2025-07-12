package kr.hhplus.be.server.concert.service;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.common.exceptions.NotFoundException;
import kr.hhplus.be.server.common.messages.MessageCode;
import kr.hhplus.be.server.concert.domain.VenueEntity;
import kr.hhplus.be.server.concert.controller.dto.VenueCreateDto;
import kr.hhplus.be.server.concert.controller.dto.VenueInfoDto;
import kr.hhplus.be.server.concert.repository.VenueJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VenueService {

    private final VenueJpaRepository repository;

    @Transactional
    public VenueCreateDto.Response createVenue(VenueCreateDto.Request request) {
        VenueEntity savedVenue = repository.save(request.toEntity());
        return VenueCreateDto.Response.fromEntity(savedVenue);
    }

    public VenueInfoDto.Response findVenue(Long venueId) {
        VenueEntity venueEntity = repository.findById(venueId).orElseThrow(() -> new NotFoundException(MessageCode.VENUE_NOT_FOUND, venueId));
        return VenueInfoDto.Response.fromEntity(venueEntity);
    }
}
