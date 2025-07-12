package kr.hhplus.be.server.concert.controller;

import kr.hhplus.be.server.concert.controller.dto.ConcertCreateDto;
import kr.hhplus.be.server.concert.controller.dto.ConcertInfoDto;
import kr.hhplus.be.server.concert.controller.dto.ConcertUpdateDto;
import kr.hhplus.be.server.concert.service.ConcertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/concerts")
public class ConcertController {

    private final ConcertService service;

    @GetMapping("/{id}")
    public ConcertInfoDto.Response getConcertInfo(@PathVariable("id") long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<ConcertCreateDto.Response> createConcert(@RequestBody ConcertCreateDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createConcert(request));
    }

    @PatchMapping
    public ResponseEntity<Long> updateConcert(@RequestBody ConcertUpdateDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.updateConcertInfo(request));
    }

    @DeleteMapping("/{id}")
    public void createConcert(@PathVariable("id") long id) {
        service.deleteConcertInfo(id);
    }


}
