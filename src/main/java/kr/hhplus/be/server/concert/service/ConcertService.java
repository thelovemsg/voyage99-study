package kr.hhplus.be.server.concert.service;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.common.exceptions.NotFoundException;
import kr.hhplus.be.server.common.messages.MessageCode;
import kr.hhplus.be.server.concert.controller.dto.ConcertCreateDto;
import kr.hhplus.be.server.concert.controller.dto.ConcertInfoDto;
import kr.hhplus.be.server.concert.domain.ConcertEntity;
import kr.hhplus.be.server.concert.controller.dto.ConcertUpdateDto;
import kr.hhplus.be.server.concert.repository.ConcertJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConcertService {

    private final ConcertJpaRepository repository;

    public ConcertInfoDto.Response findById(Long concertId) {
        ConcertEntity concertEntity = repository.findById(concertId).orElseThrow(() -> new NotFoundException(MessageCode.CONCERT_NOT_FOUND, concertId));
        return ConcertInfoDto.Response.fromEntity(concertEntity);
    }

    @Transactional
    public ConcertCreateDto.Response createConcert(ConcertCreateDto.Request request) {
        ConcertEntity newConcertEntity = ConcertCreateDto.Request.toEntity(request);
        ConcertEntity savedEntity = repository.save(newConcertEntity);
        return ConcertCreateDto.Response.fromEntity(savedEntity);
    }

    @Transactional
    public Long updateConcertInfo(ConcertUpdateDto.Request request) {
        ConcertEntity concertEntity = repository.findById(request.getConcertId()).orElseThrow(() -> new NotFoundException(MessageCode.CONCERT_NOT_FOUND, request.getConcertId()));
        concertEntity.updateInfo(request.getConcertName(), request.getDescription(), request.getArtistName());
        return concertEntity.getConcertId();
    }

    @Transactional
    public void deleteConcertInfo(long concertId) {
        ConcertEntity concertEntity = repository.findById(concertId).orElseThrow(() -> new NotFoundException(MessageCode.CONCERT_NOT_FOUND, concertId));
        repository.delete(concertEntity);
    }

}
