package kr.hhplus.be.server.ticket.infrastructure.web;

import kr.hhplus.be.server.ticket.application.port.in.dto.CreateTicketCommandDto;
import kr.hhplus.be.server.ticket.application.port.in.dto.GetTicketCommandDto;
import kr.hhplus.be.server.ticket.application.port.in.dto.PurchaseTicketCommandDto;
import kr.hhplus.be.server.ticket.application.port.in.dto.ReserveTicketCommandDto;
import kr.hhplus.be.server.ticket.application.service.*;
import kr.hhplus.be.server.ticket.application.service.redis.PurchaseTicketRedisServiceImpl;
import kr.hhplus.be.server.ticket.application.service.redis.ReserveTicketRedisServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ticket")
public class TicketController {

    private final GetTicketServiceImpl getTicketService;
    private final GetTicketCacheServiceImpl ticketCacheService;
    private final CreateTicketServiceImpl createTicketService;
    private final PurchaseTicketPessimisticLockServiceImpl purchaseTicketPessimisticLockService;
    private final PurchaseTicketRedisServiceImpl purchaseTicketRedisService;
    private final ReserveTicketServiceImpl reserveTicketService;
    private final ReserveTicketRedisServiceImpl reserveTicketRedisService;

    @GetMapping("/{ticketId}")
    public ResponseEntity<GetTicketCommandDto.Response> getTicket(@PathVariable("ticketId") Long ticketId) {
        GetTicketCommandDto.Response ticketResponse = getTicketService.getTicket(ticketId);
        return ResponseEntity.status(HttpStatus.OK).body(ticketResponse);
    }

    @GetMapping("/{ticketId}/cache")
    public ResponseEntity<GetTicketCommandDto.Response> getTicketCache(@PathVariable("ticketId") Long ticketId) {
        GetTicketCommandDto.Response ticketResponse = ticketCacheService.getTicket(ticketId);
        return ResponseEntity.status(HttpStatus.OK).body(ticketResponse);
    }

    @PostMapping
    public ResponseEntity<CreateTicketCommandDto.Response> createTicket(@RequestBody CreateTicketCommandDto.Request request) {
        CreateTicketCommandDto.Response createResponse = createTicketService.createTicket(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createResponse);
    }

    @PostMapping("/purchase")
    public ResponseEntity<PurchaseTicketCommandDto.Response> purchaseTicket(@RequestBody PurchaseTicketCommandDto.Request request) {
        PurchaseTicketCommandDto.Response purchase = purchaseTicketPessimisticLockService.purchaseWithPessimisticLock(request);
        return ResponseEntity.status(HttpStatus.OK).body(purchase);
    }

    @PostMapping("/purchase-redis")
    public ResponseEntity<PurchaseTicketCommandDto.Response> purchaseTicketRedis(@RequestBody PurchaseTicketCommandDto.Request request) {
        PurchaseTicketCommandDto.Response purchase = purchaseTicketRedisService.purchase(request);
        return ResponseEntity.status(HttpStatus.OK).body(purchase);
    }

    @PatchMapping("/reserve")
    public ResponseEntity<ReserveTicketCommandDto.Response> reserveTicket(@RequestBody ReserveTicketCommandDto.Request request) {
        ReserveTicketCommandDto.Response reserve = reserveTicketService.reserve(request);
        return ResponseEntity.status(HttpStatus.OK).body(reserve);
    }

    @PatchMapping("/reserve-redis")
    public ResponseEntity<ReserveTicketCommandDto.Response> reserveTicketRedis(@RequestBody ReserveTicketCommandDto.Request request) {
        ReserveTicketCommandDto.Response reserve = reserveTicketRedisService.reserve(request);
        return ResponseEntity.status(HttpStatus.OK).body(reserve);
    }

}
