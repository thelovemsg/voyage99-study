package kr.hhplus.be.server.common.messages;


import java.text.MessageFormat;

public enum MessageCode {

    //사용자
    USER_NOT_FOUND("사용자가 존재하지 않습니다. [id: {0}]"),
    USER_POINT_NOT_ENOUGH("사용자의 포인트가 충분하지 않습니다. [현재 포인트: {0}]"),

    //콘서트
    CONCERT_NOT_FOUND("콘서트가 존재하지 않습니다. [id: {0}]"),
    CONCERT_NOT_AVAILABLE_ERROR("콘서트가 이용가능한 콘서트가 아닙니다"),

    //공연장
    VENUE_NOT_FOUND("공연장이 존재하지 않습니다. [id: {0}]"),

    //좌석
    SEAT_NOT_FOUND("좌석 정보가 존재하지 않습니다. [id: {0}]"),

    //티켓
    TICKET_NOT_FOUND("티켓 정보가 존재하지 않습니다. [id: {0}]"),
    TICKET_ALREADY_RESERVED_ERROR("티켓이 이미 예약되어 있습니다. [id: {0}]"),
    TICKET_PURCHASE_ERROR("티켓 구매중 에러가 발생했습니다. [id: {0}]"),
    TICKET_RESERVATION_NOT_AVAILABLE("티켓 예약이 불가능합니다."),

    //토큰
    TOKEN_VALID("토큰이 유효합니다."),
    TOKEN_IS_NOT_VALID("토큰이 유효하지 않습니다."),
    TOKEN_NOT_FOUND("토큰을 찾을 수 없습니다. [tokenId: {0}]"),
    TOKEN_EXPIRED("토큰이 만료되었습니다. [만료시간: {0}]"),
    TOKEN_ALREADY_USED("이미 사용된 토큰입니다. [사용시간: {0}]"),
    TOKEN_INVALID_FORMAT("잘못된 토큰 형식입니다."),
    TOKEN_ISSUE_FAILED("토큰 발행에 실패했습니다. [사용자ID: {0}]"),
    TOKEN_EXTEND_FAILED("토큰 연장에 실패했습니다. [사유: {0}]"),
    TOKEN_USE_FAILED("토큰 사용에 실패했습니다. [사유: {0}]"),
    TOKEN_CRYPTO_ERROR("토큰 암호화 처리 중 오류가 발생했습니다."),
    TOKEN_ALREADY_EXISTS_FOR_USER("해당 사용자에게 이미 유효한 토큰이 존재합니다. [사용자ID: {0}]"),
    TOKEN_QUEUE_MISMATCH("토큰의 대기열 정보가 일치하지 않습니다. [예상: {0}, 실제: {1}]"),

    //콘서트스케줄
    CONCERT_SCHEDULE_NOT_FOUND("콘서트 스케줄이 존재하지 않습니다. [id: {0}]"),
    CONCERT_SCHEDULE_PLAY_TIME_NOT_PROPER_ERROR("공연장스케줄 시간이 올바르지 않습니다. [startTime: {0}, endTime: {1}]"),
    CONCERT_SCHEDULE_NOT_AVAILABLE("공연장스케줄 예약이 가능하지 않습니다."),

    //공통
    INPUT_DATE_RANGE_NOT_PROPER_ERROR("입력한 일정이 올바르지 않습니다."),

    //대기열
    QUEUE_NOT_FOUND("대기열을 찾을 수 없습니다"),
    QUEUE_CREATE_SUCCESS("큐가 성공적으로 생성되었습니다."),
    SERVICE_FULL("서비스가 만석입니다"),
    NOT_YOUR_TURN("아직 순서가 아닙니다"),
    QUEUE_ALREADY_EXISTS("이미 대기열에 등록되어 있습니다");

    private final String messageTemplate;

    MessageCode(String messageTemplate) {
        this.messageTemplate = messageTemplate;
    }

    public String format(Object... args) {
        return MessageFormat.format(messageTemplate, args);
    }

    public String getMessage() {
        return messageTemplate;
    }
}
