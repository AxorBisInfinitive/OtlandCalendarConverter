package com.inf.otland.cc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InputParserTest {
    @ParameterizedTest
    @CsvSource(value = {
        "''|0",
        "abc|0",
        "123abc|123",
        "'  -123xyz'|-123",
        "+123|123",
        "9999999999999999999|9223372036854775807",
        "-9999999999999999999|-999999999999999999",
        "123456789012345678901234|1234567890123456789"
    }, delimiter = '|')
    void parsesTimestampPrefixesAndOverflow(String text, long expected) {
        assertEquals(expected, InputParser.timestamp(text));
    }

    @Test
    void acceptsEmptyYearAndTimeFields() {
        assertEquals(new CalendarDateTime(0, 1, 1, 0, 0, 0),
            InputParser.otland("", "1", "1", "", "", ""));
    }

    @Test
    void truncatesBeforeCheckingDateCharacters() {
        assertEquals(new CalendarDateTime(0, 1, 1, 0, 0, 0),
            InputParser.otland("00000000000ignored", "001x", "001x", "000x", "000x", "000x"));
    }

    @Test
    void stopsAtTheFirstNul() {
        assertEquals(new CalendarDateTime(2026, 1, 1, 0, 0, 0),
            InputParser.otland("2026\0ignored", "1", "1", "0", "0", "0"));
    }

    @Test
    void rejectsNonAsciiDigitsAndSignsInDateFields() {
        String fullWidthYear = "\uFF12\uFF10\uFF12\uFF16";
        assertThrows(IllegalArgumentException.class,
            () -> InputParser.otland(fullWidthYear, "1", "1", "0", "0", "0"));
        assertThrows(IllegalArgumentException.class,
            () -> InputParser.otland("-1", "1", "1", "0", "0", "0"));
        assertThrows(IllegalArgumentException.class,
            () -> InputParser.otland("1", "", "1", "0", "0", "0"));
    }

    @Test
    void saturatesAnOverflowingYearBeforeValidation() {
        assertThrows(IllegalArgumentException.class,
            () -> InputParser.otland("99999999999", "1", "1", "0", "0", "0"));
    }

    @Test
    void countsUtf16CodeUnitsWhenTruncating() {
        String text = "a\uD83D\uDE00b";
        assertEquals("a\uD83D", InputParser.windowText(text, 3));
    }
}
