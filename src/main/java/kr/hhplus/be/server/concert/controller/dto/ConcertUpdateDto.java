package kr.hhplus.be.server.concert.controller.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ConcertUpdateDto {

    @Getter
    @Setter
    public static class Request {
        private Long concertId;
        private String description;
        private String concertName;
        private String artistName;
    }

    public static class Response {

    }
}
