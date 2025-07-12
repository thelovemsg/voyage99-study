package kr.hhplus.be.server.concert.controller.dto;

import kr.hhplus.be.server.concert.domain.VenueEntity;
import lombok.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VenueInfoDto {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long venueId;
        private String venueName;
        private String venueCallNumber;
        private Integer totalCapacity;
        private String description;

        public static Response fromEntity(VenueEntity entity) {
            return VenueInfoDto.Response.builder()
                    .venueId(entity.getVenueId())
                    .venueName(entity.getVenueName())
                    .venueCallNumber(entity.getVenueCallNumber())
                    .totalCapacity(entity.getTotalCapacity())
                    .description(entity.getDescription())
                    .build();
        }
    }
}
