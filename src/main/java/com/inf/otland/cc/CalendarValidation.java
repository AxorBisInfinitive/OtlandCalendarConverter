package com.inf.otland.cc;

public final class CalendarValidation {
    private static final int[] GREGORIAN_MONTHS = {31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

    private CalendarValidation() {
    }

    public static boolean isOtland(CalendarDateTime date) {
        return date.year() >= 0 && date.year() <= 99999
            && date.month() >= 1 && date.month() <= 16
            && date.day() >= 1 && date.day() <= OtlandCalendar.monthLength(date.year(), date.month() - 1)
            && date.hour() >= 0 && date.hour() <= 31
            && date.minute() >= 0 && date.minute() <= 63
            && date.second() >= 0 && date.second() <= 63;
    }

    public static boolean isGregorian(CalendarDateTime date) {
        if (date.year() < 0 || date.year() > 99999 || date.month() < 1 || date.month() > 12
            || date.day() < 1 || date.day() > GREGORIAN_MONTHS[date.month() - 1]
            || date.hour() < 0 || date.hour() > 23 || date.minute() < 0 || date.minute() > 59
            || date.second() < 0 || date.second() > 59) {
            return false;
        }
        boolean leap = date.year() % 4 == 0 && date.year() % 100 != 0 || date.year() % 400 == 0;
        return leap || date.month() != 2 || date.day() != 29;
    }
}
