package kr.hhplus.be.server.concert.controller.dto;

import kr.hhplus.be.server.concert.domain.ConcertEntity;
import lombok.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ConcertInfoDto {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long concertId;
        private String concertName;
        private String artistName;
        private String description;

        public static Response fromEntity(ConcertEntity entity) {
            return ConcertInfoDto.Response.builder()
                    .concertId(entity.getConcertId())
                    .concertName(entity.getConcertName())
                    .artistName(entity.getArtistName())
                    .description(entity.getDescription())
                    .build();
        }
    }
}
