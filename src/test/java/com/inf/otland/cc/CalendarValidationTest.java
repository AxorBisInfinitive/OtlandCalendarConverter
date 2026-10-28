package com.inf.otland.cc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalendarValidationTest {
    @ParameterizedTest
    @CsvFileSource(resources = "/date-validation.csv", numLinesToSkip = 1)
    void checksCalendarRanges(int year, int month, int day, int hour, int minute, int second,
                             boolean otland, boolean gregorian) {
        CalendarDateTime date = new CalendarDateTime(year, month, day, hour, minute, second);
        assertEquals(otland, CalendarValidation.isOtland(date));
        assertEquals(gregorian, CalendarValidation.isGregorian(date));
    }

    @Test
    void acceptsYearZeroInBothCalendars() {
        CalendarDateTime date = new CalendarDateTime(0, 1, 1, 0, 0, 0);
        assertTrue(CalendarValidation.isOtland(date));
        assertTrue(CalendarValidation.isGregorian(date));
    }

    @Test
    void limitsTheLastMonthAccordingToTheLeapYear() {
        assertFalse(CalendarValidation.isOtland(new CalendarDateTime(7, 16, 33, 0, 0, 0)));
        assertTrue(CalendarValidation.isOtland(new CalendarDateTime(8, 16, 33, 0, 0, 0)));
    }

    @Test
    void appliesGregorianCenturyRules() {
        assertFalse(CalendarValidation.isGregorian(new CalendarDateTime(1900, 2, 29, 0, 0, 0)));
        assertTrue(CalendarValidation.isGregorian(new CalendarDateTime(2000, 2, 29, 0, 0, 0)));
    }
}
