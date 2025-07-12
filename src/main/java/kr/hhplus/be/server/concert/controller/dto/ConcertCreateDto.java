package kr.hhplus.be.server.concert.controller.dto;

import kr.hhplus.be.server.concert.domain.ConcertEntity;
import lombok.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ConcertCreateDto {

    @Getter
    @Setter
    public static class Request {
        private String description;
        private String artistName;
        private String concertName;

        public static ConcertEntity toEntity(Request request) {
            return ConcertEntity.builder()
                    .description(request.getDescription())
                    .artistName(request.getArtistName())
                    .concertName(request.getConcertName())
                    .build();
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {
        private Long concertId;

        public static ConcertCreateDto.Response fromEntity(ConcertEntity savedEntity) {
            return new Response(savedEntity.getConcertId());
        }
    }

}
