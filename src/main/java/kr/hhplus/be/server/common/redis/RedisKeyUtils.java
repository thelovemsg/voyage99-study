package kr.hhplus.be.server.common.redis;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

import static kr.hhplus.be.server.common.redis.RedisPrefixEnum.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RedisKeyUtils {

    // 상수로 미리 추출
    private static final String COLON_STR = COLON.getPrefix();
    private static final String LOCK_STR = LOCK.getPrefix();
    private static final String TICKET_STR = TICKET.getPrefix();
    private static final String RESERVE_STR = RESERVE.getPrefix();
    private static final String PURCHASE_STR = PURCHASE.getPrefix();
    private static final String USER_STR = USER.getPrefix();
    private static final String REMAINING_STR = REMAINING.getPrefix();
    private static final String SCHEDULE_STR = SCHEDULE.getPrefix();
    private static final String RANK_STR = RANK.getPrefix();        // 수정
    private static final String DAILY_STR = DAILY.getPrefix();      // 추가
    private static final String QUEUE_STR = QUEUE.getPrefix();      // 추가

    // 티켓 잠금 키 생성
    public static String getTicketReverseLockKey(Long ticketId) {
        return String.join(COLON_STR, LOCK_STR, TICKET_STR, RESERVE_STR, ticketId.toString());
    }

    public static String getTicketPurchaseLockKey(Long ticketId) {
        return String.join(COLON_STR, LOCK_STR, TICKET_STR, PURCHASE_STR, ticketId.toString());
    }

    // 다른 티켓 관련 키도 추가 가능
    public static String getTicketInfoKey(Long ticketId) {
        return String.join(COLON_STR, TICKET_STR, ticketId.toString());
    }

    // 다른 도메인의 키도 추가 가능
    public static String getUserLockKey(Long userId) {
        return String.join(COLON_STR, LOCK_STR, USER_STR, userId.toString());
    }

    public static String getConcertScheduleTotalNumberKey(Long concertScheduleId) {
        return String.join(COLON_STR, REMAINING_STR, SCHEDULE_STR, concertScheduleId.toString());
    }

    // 잔여 좌석 키
    public static String getConcertScheduleRemainingKey(Long concertScheduleId) {
        return String.join(COLON_STR, REMAINING_STR, SCHEDULE_STR, concertScheduleId.toString());
    }

    // 일일 랭킹 키 (오늘 날짜 기준)
    public static String getDailyRankingKey() {
        String today = LocalDate.now().toString(); // 2024-12-25
        return String.join(COLON_STR, DAILY_STR, RANK_STR, today);
    }

    // 특정 날짜 랭킹 키
    public static String getDailyRankingKey(LocalDate date) {
        return String.join(COLON_STR, DAILY_STR, RANK_STR, date.toString());
    }

    public static String getQueueWaitingKey() {
        return String.join(COLON_STR, QUEUE_STR);
    }

}