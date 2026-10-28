package com.inf.otland.cc;

public final class InputParser {
    private InputParser() {
    }

    public static CalendarDateTime otland(String... fields) {
        CalendarDateTime date = dateFields(fields);
        if (!CalendarValidation.isOtland(date)) {
            throw invalidDate();
        }
        return date;
    }

    public static CalendarDateTime gregorian(String... fields) {
        CalendarDateTime date = dateFields(fields);
        if (!CalendarValidation.isGregorian(date)) {
            throw invalidDate();
        }
        return date;
    }

    public static long timestamp(String text) {
        return CrtRuntime.parseLong(windowText(text, 20));
    }

    private static CalendarDateTime dateFields(String[] fields) {
        if (fields.length != 6) {
            throw new IllegalArgumentException("Expected six date and time fields.");
        }
        String[] texts = new String[6];
        for (int index = 0; index < fields.length; index++) {
            texts[index] = windowText(fields[index], index == 0 ? 12 : 4);
            for (int character = 0; character < texts[index].length(); character++) {
                char value = texts[index].charAt(character);
                if (value < '0' || value > '9') {
                    throw invalidDate();
                }
            }
        }
        int[] values = new int[6];
        for (int index = 0; index < values.length; index++) {
            values[index] = CrtRuntime.parseInt(texts[index]);
        }
        return new CalendarDateTime(values[0], values[1], values[2], values[3], values[4], values[5]);
    }

    static String windowText(String text, int capacity) {
        int nul = text.indexOf('\0');
        int length = Math.min(nul < 0 ? text.length() : nul, capacity - 1);
        return text.substring(0, length);
    }

    static IllegalArgumentException invalidDate() {
        return new IllegalArgumentException("Invalid date or time.");
    }
}
