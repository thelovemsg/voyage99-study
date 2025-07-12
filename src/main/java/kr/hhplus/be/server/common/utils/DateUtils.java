package kr.hhplus.be.server.common.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DateUtils {

    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    private static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DEFAULT_DATE_TIME_FORMAT_NO_SPLIT = "yyyyMMddHHmmss";

    /**
     * 날짜/시간 객체를 문자열로 변환
     *
     * @param target         변환할 날짜/시간 객체
     * @param inputFormatter 포맷 패턴 (null이면 기본 포맷 사용)
     * @param <T>            LocalDate 또는 LocalDateTime
     * @return 포맷된 문자열
     */
    public static <T> String inputToString(T target, String inputFormatter) {

        if (ObjectUtils.isEmpty(target)) {
            return null;
        }

        String formatPattern = StringUtils.hasText(inputFormatter) ? inputFormatter : getDefaultFormat(target);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatPattern);

        if (target instanceof LocalDateTime localDateTime) {
            return localDateTime.format(formatter);
        }

        if (target instanceof LocalDate localDate) {
            return localDate.format(formatter);
        }

        // 지원하지 않는 타입
        throw new IllegalArgumentException("Unsupported type: " + target.getClass().getSimpleName());
    }

    public static String getDefaultDateTime() {
        return inputToString(LocalDateTime.now(), DEFAULT_DATE_TIME_FORMAT);
    }

    public static String getDefaultDateTimeNoSplit() {
        return inputToString(LocalDateTime.now(), DEFAULT_DATE_TIME_FORMAT_NO_SPLIT);
    }

    /**
     * 객체 타입에 따른 기본 포맷 반환
     */
    private static <T> String getDefaultFormat(T target) {
        if (target instanceof LocalDateTime) {
            return DEFAULT_DATE_TIME_FORMAT;
        }
        if (target instanceof LocalDate) {
            return DEFAULT_DATE_FORMAT;
        }
        return DEFAULT_DATE_FORMAT; // fallback
    }

    public static String localDateToString(LocalDate date) {
        return inputToString(date, null);
    }

    public static String localDateTimeToString(LocalDateTime dateTime) {
        return inputToString(dateTime, null);
    }

    public static String localDateToString(LocalDate date, String pattern) {
        return inputToString(date, pattern);
    }

    public static String localDateTimeToString(LocalDateTime dateTime, String pattern) {
        return inputToString(dateTime, pattern);
    }
}