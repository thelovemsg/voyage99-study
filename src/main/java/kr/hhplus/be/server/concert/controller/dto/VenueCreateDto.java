package kr.hhplus.be.server.concert.controller.dto;

import kr.hhplus.be.server.concert.domain.VenueEntity;
import lombok.*;

import java.io.Serializable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VenueCreateDto {

    @Getter
    @Setter
    public static class Request {
        String venueName;
        String venueAddress;
        String venueCallNumber;
        Integer totalCapacity;
        String description;

        public VenueEntity toEntity() {
            return VenueEntity.builder()
                    .venueName(this.venueName)
                    .venueAddress(this.venueAddress)
                    .venueCallNumber(this.venueCallNumber)
                    .totalCapacity(this.totalCapacity)
                    .description(this.description)
                    .build();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long venueId;

        public static Response fromEntity(VenueEntity savedVenue) {
            return new Response(savedVenue.getVenueId());
        }
    }
}