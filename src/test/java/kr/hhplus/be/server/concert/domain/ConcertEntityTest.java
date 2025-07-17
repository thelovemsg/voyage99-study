package kr.hhplus.be.server.concert.domain;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConcertEntityTest {

    @Test
    @DisplayName("콘서트 생성 테스트 - 첫 생성 콘서트는 사용 불가")
    void 콘서트_생성() {
        ConcertEntity concert = ConcertEntity.builder()
                .concertName("항해99-코딩 콘서트")
                .artistName("니노막스무스카이저소제")
                .description("항해99 코딩 콘서트입니다.")
                .build();

        Assertions.assertThat(concert.isConcertAvailable()).isFalse();
    }

    @Test
    @DisplayName("콘서트 생성 테스트 - 첫 생성 콘서트는 사용 불가")
    void 콘서트_생성_사용가능() {
        ConcertEntity concert = ConcertEntity.builder()
                .concertName("항해99-코딩 콘서트")
                .artistName("니노막스무스카이저소제")
                .description("항해99 코딩 콘서트입니다.")
                .build();

        concert.openConcert();

        Assertions.assertThat(concert.isConcertAvailable()).isTrue();
    }

}