package kr.hhplus.be.server.concert.enums;

/**
 * NOT_ALLOWED : 불허(사용금지 - 모종의 이유로 사용을 막음)
 * FIXING : 수리중(사용금지 - 현재 수리중이라 사용이 금지)
 * GOOD : 좋음(사용가능) - 기본 상태
 * OCCUPIED : 점유(누군가의 예약 혹은 지정석이라 사용 불가능)
 */
public enum SeatStatusEnum {
    FIXING, GOOD, NOT_ALLOWED, OCCUPIED;
}
