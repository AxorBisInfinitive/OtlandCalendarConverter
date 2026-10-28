package com.inf.otland.cc;

public final class CrtRuntime {
    public static final long MAX_TIMESTAMP = 0x793582AFFL;
    public static final CalendarDateTime INVALID_DATE = new CalendarDateTime(1899, 0, -1, -1, -1, -1);
    private static final int[] MONTH_LENGTHS = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    private static final int[] DIGIT_ZEROS = {
        0x0030, 0x0660, 0x06F0, 0x0966, 0x09E6, 0x0A66, 0x0AE6, 0x0B66,
        0x0C66, 0x0CE6, 0x0D66, 0x0E50, 0x0ED0, 0x0F20, 0x1040, 0x17E0, 0x1810, 0xFF10
    };
    private final CrtTimeZone timeZone;

    public CrtRuntime(CrtTimeZone timeZone) {
        this.timeZone = java.util.Objects.requireNonNull(timeZone);
    }

    static CrtRuntime instance() {
        return Holder.INSTANCE;
    }

    public static int parseInt(String text) {
        return (int) parseNumber(text, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public static long parseLong(String text) {
        return parseNumber(text, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    private static long parseNumber(String text, long minimum, long maximum) {
        int index = 0;
        while (index < text.length() && space(text.charAt(index))) {
            index++;
        }
        boolean negative = index < text.length() && text.charAt(index) == '-';
        if (index < text.length() && (text.charAt(index) == '+' || text.charAt(index) == '-')) {
            index++;
        }
        long limit = negative ? minimum : -maximum;
        long beforeMultiply = limit / 10;
        long result = 0;
        while (index < text.length()) {
            int digit = digit(text.charAt(index++));
            if (digit < 0) {
                break;
            }
            if (result < beforeMultiply) {
                return negative ? minimum : maximum;
            }
            result *= 10;
            if (result < limit + digit) {
                return negative ? minimum : maximum;
            }
            result -= digit;
        }
        return negative ? result : -result;
    }

    private static int digit(char value) {
        for (int zero : DIGIT_ZEROS) {
            if (value >= zero && value <= zero + 9) {
                return value - zero;
            }
        }
        return -1;
    }

    private static boolean space(char value) {
        return value >= 9 && value <= 13 || value == 0x20 || value == 0x85 || value == 0xA0
            || value == 0x1680 || value == 0x180E || value >= 0x2000 && value <= 0x200A
            || value == 0x2028 || value == 0x2029 || value == 0x202F || value == 0x205F || value == 0x3000;
    }

    public long toTimestamp(CalendarDateTime date) {
        long relativeYear = date.year() - 1900;
        if (relativeYear < 69 || relativeYear > 1102) {
            return -1;
        }
        int month = date.month() - 1;
        relativeYear += month / 12;
        month %= 12;
        if (month < 0) {
            month += 12;
            relativeYear--;
        }
        if (relativeYear < 69 || relativeYear > 1102) {
            return -1;
        }
        CalendarDateTime normalized = new CalendarDateTime((int) relativeYear + 1900,
            month + 1, date.day(), date.hour(), date.minute(), date.second());
        long timestamp = localSeconds(normalized) - timeZone.standardOffset();
        return fromTimestamp(timestamp).equals(INVALID_DATE) ? -1 : timestamp;
    }

    public CalendarDateTime fromTimestamp(long timestamp) {
        if (timestamp < 0 || timestamp > MAX_TIMESTAMP) {
            return INVALID_DATE;
        }
        long local = timestamp + timeZone.standardOffset();
        if (timestamp > 3 * 86400L && timestamp < MAX_TIMESTAMP - 3 * 86400L) {
            CalendarDateTime standard = utcDate(local);
            if (standard.equals(INVALID_DATE)) {
                return INVALID_DATE;
            }
            return timeZone.isDaylight(standard)
                ? utcDate(local + timeZone.daylightAdjustment()) : standard;
        }
        // Boundary conversions test daylight time before shifting the UTC fields.
        CalendarDateTime utc = utcDate(timestamp);
        boolean daylight = timeZone.isDaylight(utc);
        return shiftBoundary(utc, timeZone.standardOffset() + (daylight ? timeZone.daylightAdjustment() : 0));
    }

    public long now() {
        return Math.floorDiv(System.currentTimeMillis(), 1000);
    }

    private static CalendarDateTime utcDate(long seconds) {
        return seconds < -43200 || seconds > MAX_TIMESTAMP + 50400
            ? INVALID_DATE : splitSeconds(seconds);
    }

    private static CalendarDateTime shiftBoundary(CalendarDateTime utc, long adjustment) {
        long seconds = utc.second() + adjustment;
        long minutes = utc.minute() + Math.floorDiv(seconds, 60);
        long hours = utc.hour() + Math.floorDiv(minutes, 60);
        long days = Math.floorDiv(hours, 24);
        int day = utc.day() + (int) days;
        int month = utc.month();
        int year = utc.year();
        if (days < 0 && day <= 0) {
            day += 31;
            month = 12;
            year--;
        }
        return new CalendarDateTime(year, month, day, (int) Math.floorMod(hours, 24),
            (int) Math.floorMod(minutes, 60), (int) Math.floorMod(seconds, 60));
    }

    private static CalendarDateTime splitSeconds(long seconds) {
        long days = Math.floorDiv(seconds, 86400);
        int secondOfDay = (int) Math.floorMod(seconds, 86400);
        int year = (int) (1970 + Math.floorDiv(days, 365));
        while (days < daysBeforeYear(year)) {
            year--;
        }
        while (days >= daysBeforeYear(year + 1)) {
            year++;
        }
        int dayOfYear = (int) (days - daysBeforeYear(year));
        int month = 1;
        while (dayOfYear >= monthLength(year, month)) {
            dayOfYear -= monthLength(year, month++);
        }
        return new CalendarDateTime(year, month, dayOfYear + 1,
            secondOfDay / 3600, secondOfDay / 60 % 60, secondOfDay % 60);
    }

    static long localSeconds(CalendarDateTime date) {
        long days = daysBeforeYear(date.year());
        for (int month = 1; month < date.month(); month++) {
            days += monthLength(date.year(), month);
        }
        days += (long) date.day() - 1;
        return ((days * 24 + date.hour()) * 60 + date.minute()) * 60 + date.second();
    }

    private static long daysBeforeYear(int year) {
        long previous = (long) year - 1;
        long leaps = Math.floorDiv(previous, 4) - Math.floorDiv(previous, 100) + Math.floorDiv(previous, 400);
        return ((long) year - 1970) * 365 + leaps - 477;
    }

    static int monthLength(int year, int month) {
        boolean leap = year % 4 == 0 && year % 100 != 0 || year % 400 == 0;
        return month == 2 && leap ? 29 : MONTH_LENGTHS[month - 1];
    }

    private static final class Holder {
        private static final CrtRuntime INSTANCE = new CrtRuntime(CrtTimeZone.systemDefault());
    }
}
