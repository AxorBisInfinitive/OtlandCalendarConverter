package com.inf.otland.cc;

import java.util.Locale;

public record CalendarDateTime(int year, int month, int day, int hour, int minute, int second) {
    public String format() {
        return String.format(Locale.ROOT, "%04d-%02d-%02d %02d:%02d:%02d",
            year, month, day, hour, minute, second);
    }
}
