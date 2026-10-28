package com.inf.otland.cc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OtlandCalendarTest {
    @ParameterizedTest
    @CsvFileSource(resources = "/otland-encode.csv", numLinesToSkip = 1)
    void convertsDateFields(int year, int month, int day, int hour, int minute, int second,
                            long internal, long timestamp) {
        CalendarDateTime date = new CalendarDateTime(year, month, day, hour, minute, second);
        OtlandCalendar calendar = new OtlandCalendar().setFields(date);
        assertEquals(date, calendar.date());
        assertEquals(internal, calendar.getTime(false));
        assertEquals(timestamp, calendar.getTime(true));
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/otland-decode.csv", numLinesToSkip = 1)
    void convertsInternalSeconds(long internal, int year, int month, int day,
                                 int hour, int minute, int second, long remaining, long unixRemaining) {
        CalendarDateTime expected = new CalendarDateTime(year, month, day, hour, minute, second);
        OtlandCalendar calendar = new OtlandCalendar().setInternal(internal);
        assertEquals(expected, calendar.date());
        assertEquals(remaining, calendar.getTime(false));
        assertEquals(unixRemaining, calendar.getTime(true));
        assertEquals(expected, CalendarConverter.timestampToOtland(internal - OtlandCalendar.UNIX_OFFSET));
    }

    @Test
    void initializesFieldsAndInternalTime() {
        OtlandCalendar calendar = new OtlandCalendar();
        assertEquals(new CalendarDateTime(0, 0, 0, 0, 0, 0), calendar.date());
        assertEquals(0x224C16A000L, calendar.getTime(false));
    }

    @Test
    void leavesDayZeroAtTheLeapYearBoundary() {
        long timestamp = CalendarConverter.otlandToTimestamp(new CalendarDateTime(8, 16, 33, 0, 0, 0));
        assertEquals(new CalendarDateTime(9, 1, 0, 0, 0, 0), CalendarConverter.timestampToOtland(timestamp));
    }

    @Test
    void usesTheSuppliedDayWithoutSubtractingOne() {
        long timestamp = CalendarConverter.otlandToTimestamp(new CalendarDateTime(1, 1, 1, 0, 0, 0));
        assertEquals(OtlandCalendar.DAY_SECONDS - OtlandCalendar.UNIX_OFFSET, timestamp);
        assertEquals(new CalendarDateTime(1, 1, 0, 0, 0, 0),
            CalendarConverter.timestampToOtland(-OtlandCalendar.UNIX_OFFSET));
    }

    @Test
    void keepsSignedRemaindersForNegativeSeconds() {
        assertEquals(new CalendarDateTime(1, 1, 0, 0, -1, -1),
            new OtlandCalendar().setInternal(-65).date());
    }
}
