package com.inf.otland.cc;

public final class CalendarConverter {
    public record Conversion(long timestamp, CalendarDateTime gregorian, CalendarDateTime otland) {
    }

    private CalendarConverter() {
    }

    public static long otlandToTimestamp(CalendarDateTime date) {
        if (!CalendarValidation.isOtland(date)) {
            throw InputParser.invalidDate();
        }
        return new OtlandCalendar().setFields(date).getTime(true);
    }

    public static CalendarDateTime timestampToOtland(long timestamp) {
        return new OtlandCalendar().setInternal(timestamp + OtlandCalendar.UNIX_OFFSET).date();
    }

    public static long gregorianToTimestamp(CalendarDateTime date) {
        if (!CalendarValidation.isGregorian(date)) {
            throw InputParser.invalidDate();
        }
        return CrtRuntime.instance().toTimestamp(date);
    }

    public static CalendarDateTime timestampToGregorian(long timestamp) {
        return CrtRuntime.instance().fromTimestamp(timestamp);
    }

    public static CalendarDateTime gregorianToOtland(CalendarDateTime date) {
        return timestampToOtland(gregorianToTimestamp(date));
    }

    public static CalendarDateTime otlandToGregorian(CalendarDateTime date) {
        return timestampToGregorian(otlandToTimestamp(date));
    }

    public static Conversion fromTimestamp(long timestamp) {
        return new Conversion(timestamp, timestampToGregorian(timestamp), timestampToOtland(timestamp));
    }

    public static Conversion fromTimestampText(String text) {
        return fromTimestamp(InputParser.timestamp(text));
    }

    public static Conversion fromOtlandFields(String... fields) {
        CalendarDateTime date = InputParser.otland(fields);
        long timestamp = otlandToTimestamp(date);
        return new Conversion(timestamp, timestampToGregorian(timestamp), date);
    }

    public static Conversion fromGregorianFields(String... fields) {
        CalendarDateTime date = InputParser.gregorian(fields);
        long timestamp = gregorianToTimestamp(date);
        return new Conversion(timestamp, date, timestampToOtland(timestamp));
    }

    public static Conversion now() {
        return fromTimestamp(CrtRuntime.instance().now());
    }
}
