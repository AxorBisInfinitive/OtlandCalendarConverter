package com.inf.otland.cc;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppTest {
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errors = new ByteArrayOutputStream();

    private int run(String... args) {
        return App.run(args, new PrintStream(output, true, StandardCharsets.UTF_8),
            new PrintStream(errors, true, StandardCharsets.UTF_8));
    }

    @Test
    void showsUsage() {
        assertEquals(0, run("--help"));
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Usage:"));
        assertEquals(0, errors.size());
    }

    @Test
    void printsBothCalendarsForATimestamp() {
        assertEquals(0, run("--timestamp", "0"));
        String text = output.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("Timestamp: 0"));
        assertTrue(text.matches("(?s).*Gregorian: [0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}.*"));
        assertTrue(text.contains("Otland: 2169-13-23 00:63:32"));
        assertEquals(0, errors.size());
    }

    @Test
    void convertsOtlandFields() {
        assertEquals(0, run("--otland", "2196", "1", "1", "0", "0", "0"));
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Timestamp: 1759113248"));
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Otland: 2196-01-01 00:00:00"));
    }

    @Test
    void acceptsANegativeTimestampArgument() {
        assertEquals(0, run("--timestamp", "-1"));
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Gregorian: 1899-00--1 -1:-1:-1"));
    }

    @Test
    void rejectsAnUnknownCommand() {
        assertEquals(2, run("--unknown"));
        assertTrue(errors.toString(StandardCharsets.UTF_8).contains("Unknown command:"));
        assertEquals(0, output.size());
    }

    @Test
    void rejectsMissingArguments() {
        assertEquals(2, run("--otland", "2196"));
        assertTrue(errors.toString(StandardCharsets.UTF_8).contains("Incorrect number of arguments."));
    }

    @Test
    void rejectsInvalidDates() {
        assertEquals(2, run("--gregorian", "2026", "2", "29", "0", "0", "0"));
        assertTrue(errors.toString(StandardCharsets.UTF_8).contains("Invalid date or time."));
    }
}
