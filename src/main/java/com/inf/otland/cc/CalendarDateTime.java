package com.inf.otland.cc;

public record CalendarDateTime(int year, int month, int day, int hour, int minute, int second) {
    public String format() {
        return year + " " + month + " " + day + " " + hour + " " + minute + " " + second;
    }
}
