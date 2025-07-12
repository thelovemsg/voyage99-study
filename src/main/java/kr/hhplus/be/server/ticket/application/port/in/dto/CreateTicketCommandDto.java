package kr.hhplus.be.server.ticket.application.port.in.dto;

import kr.hhplus.be.server.ticket.domain.model.Ticket;
import lombok.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CreateTicketCommandDto {

    @Getter
    @Setter
    public static class Request {
        private Long seatId;
        private Long concertScheduleId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long ticketId;

        public static Response fromDomain(Ticket savedTicket) {
            return new Response(savedTicket.getTicketId());
        }
    }
}
