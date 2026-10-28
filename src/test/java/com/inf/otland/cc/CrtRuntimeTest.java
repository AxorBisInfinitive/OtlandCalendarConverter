package com.inf.otland.cc;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrtRuntimeTest {
    @ParameterizedTest
    @CsvFileSource(resources = "/gregorian-encode.csv", numLinesToSkip = 1)
    void convertsGregorianInput(String zone, int year, int month, int day,
                               int hour, int minute, int second, long timestamp) {
        CrtRuntime runtime = new CrtRuntime(CrtTimeZone.of(zone));
        assertEquals(timestamp, runtime.toTimestamp(new CalendarDateTime(year, month, day, hour, minute, second)));
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/gregorian-decode.csv", numLinesToSkip = 1)
    void convertsTimestampsToLocalTime(String zone, long timestamp, int year, int month, int day,
                                      int hour, int minute, int second) {
        CrtRuntime runtime = new CrtRuntime(CrtTimeZone.of(zone));
        assertEquals(new CalendarDateTime(year, month, day, hour, minute, second), runtime.fromTimestamp(timestamp));
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/number-parsing.csv", numLinesToSkip = 1)
    void parsesSignedNumbers(String encoded, int expectedInt, long expectedLong) {
        String text = decode(encoded);
        assertEquals(expectedInt, CrtRuntime.parseInt(text));
        assertEquals(expectedLong, CrtRuntime.parseLong(text));
    }

    @Test
    void checksEveryUtf16Prefix() throws Exception {
        var stream = getClass().getResourceAsStream("/number-prefixes.csv");
        assertNotNull(stream);
        try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            reader.readLine();
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");
                String text = Character.toString((char) Integer.parseInt(fields[0], 16)) + "7";
                int expected = Integer.parseInt(fields[1]);
                assertEquals(expected, CrtRuntime.parseInt(text), fields[0]);
                assertEquals((long) expected, CrtRuntime.parseLong(text), fields[0]);
                count++;
            }
            assertEquals(65536, count);
        }
    }

    @Test
    void carriesTheFailureTimestampIntoTheOtherCalendar() {
        CalendarConverter.Conversion result = CalendarConverter.fromGregorianFields("0", "1", "1", "0", "0", "0");
        assertEquals(-1, result.timestamp());
        assertEquals(new CalendarDateTime(0, 1, 1, 0, 0, 0), result.gregorian());
        assertEquals(CalendarConverter.timestampToOtland(-1), result.otland());
    }

    @Test
    void keepsTheInputDateAtTheLeapYearBoundary() {
        CalendarConverter.Conversion result = CalendarConverter.fromOtlandFields("8", "16", "33", "0", "0", "0");
        assertEquals(new CalendarDateTime(8, 16, 33, 0, 0, 0), result.otland());
        assertEquals(CrtRuntime.INVALID_DATE, result.gregorian());
    }

    @Test
    void usesStandardTimeForSummerInput() {
        CrtRuntime runtime = new CrtRuntime(CrtTimeZone.of("PST8PDT"));
        CalendarDateTime input = new CalendarDateTime(2026, 7, 15, 12, 0, 0);
        long timestamp = runtime.toTimestamp(input);
        assertEquals(new CalendarDateTime(2026, 7, 15, 13, 0, 0), runtime.fromTimestamp(timestamp));
    }

    @Test
    void acceptsTheLastRepresentableSecond() {
        CrtRuntime runtime = new CrtRuntime(CrtTimeZone.of("UTC0"));
        CalendarDateTime last = new CalendarDateTime(3001, 1, 19, 7, 59, 59);
        assertEquals(last, runtime.fromTimestamp(32_536_799_999L));
        assertEquals(32_536_799_999L, runtime.toTimestamp(last));
        assertEquals(CrtRuntime.INVALID_DATE, runtime.fromTimestamp(32_536_800_000L));
        assertEquals(-1, runtime.toTimestamp(new CalendarDateTime(3001, 1, 19, 8, 0, 0)));
    }

    @Test
    void normalizesRawDateFields() {
        CrtRuntime runtime = new CrtRuntime(CrtTimeZone.of("UTC0"));
        assertEquals(runtime.toTimestamp(new CalendarDateTime(2026, 2, 4, 0, 0, 0)),
            runtime.toTimestamp(new CalendarDateTime(2026, 1, 35, 0, 0, 0)));
        assertEquals(runtime.toTimestamp(new CalendarDateTime(2025, 12, 31, 23, 59, 59)),
            runtime.toTimestamp(new CalendarDateTime(2026, 1, 1, 0, 0, -1)));
    }

    @Test
    void usesCurrentAnnualRulesForNamedZones() {
        CrtRuntime shanghai = new CrtRuntime(CrtTimeZone.fromZoneId(ZoneId.of("Asia/Shanghai")));
        assertEquals(new CalendarDateTime(1970, 1, 1, 8, 0, 0), shanghai.fromTimestamp(0));
        CalendarDateTime summer = new CalendarDateTime(1988, 7, 1, 12, 0, 0);
        assertEquals(summer, shanghai.fromTimestamp(shanghai.toTimestamp(summer)));
        CrtRuntime sydney = new CrtRuntime(CrtTimeZone.fromZoneId(ZoneId.of("Australia/Sydney")));
        CalendarDateTime january = new CalendarDateTime(2026, 1, 15, 12, 0, 0);
        assertEquals(new CalendarDateTime(2026, 1, 15, 13, 0, 0),
            sydney.fromTimestamp(sydney.toTimestamp(january)));
        CrtRuntime lordHowe = new CrtRuntime(CrtTimeZone.fromZoneId(ZoneId.of("Australia/Lord_Howe")));
        assertEquals(new CalendarDateTime(2026, 1, 15, 12, 30, 0),
            lordHowe.fromTimestamp(lordHowe.toTimestamp(january)));
    }

    @Test
    void readsTheCurrentUnixTime() {
        long before = System.currentTimeMillis() / 1000;
        CalendarConverter.Conversion result = CalendarConverter.now();
        long after = System.currentTimeMillis() / 1000;
        assertTrue(result.timestamp() >= before && result.timestamp() <= after);
        assertEquals(CalendarConverter.timestampToOtland(result.timestamp()), result.otland());
    }

    private static String decode(String encoded) {
        if (encoded.equals("empty")) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (String unit : encoded.split(" ")) {
            text.append((char) Integer.parseInt(unit, 16));
        }
        return text.toString();
    }
}
