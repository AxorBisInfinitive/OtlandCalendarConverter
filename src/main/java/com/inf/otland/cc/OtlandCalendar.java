package com.inf.otland.cc;

public final class OtlandCalendar {
    public static final long UNIX_OFFSET = 145_580_888_032L;
    public static final long DAY_SECONDS = 131_072L;
    private static final short[] COMMON_MONTHS = {
        33, 32, 31, 32, 33, 32, 31, 32, 33, 32, 31, 32, 33, 32, 31, 32
    };
    private static final short[] LEAP_MONTHS = {
        33, 32, 31, 32, 33, 32, 31, 32, 33, 32, 31, 32, 33, 32, 31, 33
    };

    private int year;
    private int month;
    private int day;
    private int hour;
    private int minute;
    private int second;
    private long internal = 0x224C16A000L;

    public CalendarDateTime date() {
        return new CalendarDateTime(year, month, day, hour, minute, second);
    }

    public long getTime(boolean unix) {
        return unix ? internal - UNIX_OFFSET : internal;
    }

    public OtlandCalendar setFields(CalendarDateTime date) {
        year = date.year();
        month = date.month();
        day = date.day();
        hour = date.hour();
        minute = date.minute();
        second = date.second();
        recalculate();
        return this;
    }

    public void recalculate() {
        internal = second + (long) minute * 64 + (long) hour * 4096 + day * DAY_SECONDS;
        int limit = month - 1;
        if (limit > 16) {
            throw new IllegalArgumentException("Month index exceeds the calendar table.");
        }
        for (short index = 0; index < limit; index++) {
            internal += monthLength(year, index) * DAY_SECONDS;
        }
        int previousYear = year - 1;
        internal += (long) previousYear * 512 * DAY_SECONDS;
        internal += (long) (previousYear / 8) * DAY_SECONDS;
    }

    public OtlandCalendar setInternal(long value) {
        internal = value;
        second = (int) (internal % 64);
        internal /= 64;
        minute = (int) (internal % 64);
        internal /= 64;
        hour = (int) (internal % 32);
        internal /= 32;
        year = (int) (internal / 513);
        internal %= 513;
        internal += year - year / 8;
        year++;
        while (internal >= 513) {
            internal -= year % 8 == 0 ? 513 : 512;
            year++;
        }
        month = 0;
        while (internal > monthLength(year, month)) {
            internal -= monthLength(year, month);
            if (month >= 15) {
                month = 1;
                year++;
            } else {
                month++;
            }
        }
        month++;
        day = (int) internal;
        return this;
    }

    static int monthLength(int year, int index) {
        return (year % 8 == 0 ? LEAP_MONTHS : COMMON_MONTHS)[index];
    }
}
