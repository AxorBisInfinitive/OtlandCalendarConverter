package com.inf.otland.cc;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneOffsetTransitionRule;
import java.time.zone.ZoneRules;
import java.util.List;

public final class CrtTimeZone {
    private final long standardOffset;
    private final int daylightAdjustment;
    private final DaylightRule daylightRule;

    private CrtTimeZone(long standardOffset, int daylightAdjustment, DaylightRule daylightRule) {
        this.standardOffset = standardOffset;
        this.daylightAdjustment = daylightAdjustment;
        this.daylightRule = daylightRule;
    }

    public static CrtTimeZone fixedOffset(int secondsEast) {
        return new CrtTimeZone(secondsEast, 0, date -> false);
    }

    public static CrtTimeZone of(String specification) {
        if (specification.startsWith(":")) {
            return fromZoneId(ZoneId.of(specification.substring(1)));
        }
        if (specification.contains("/")) {
            return fromZoneId(ZoneId.of(specification));
        }
        int index = Math.min(3, specification.length());
        boolean negative = index < specification.length() && specification.charAt(index) == '-';
        if (negative) {
            index++;
        }
        int west = CrtRuntime.parseInt(specification.substring(index)) * 3600;
        while (index < specification.length()
                && (specification.charAt(index) == '+' || asciiDigit(specification.charAt(index)))) {
            index++;
        }
        if (index < specification.length() && specification.charAt(index) == ':') {
            west += CrtRuntime.parseInt(specification.substring(++index)) * 60;
            while (index < specification.length() && asciiDigit(specification.charAt(index))) {
                index++;
            }
            if (index < specification.length() && specification.charAt(index) == ':') {
                west += CrtRuntime.parseInt(specification.substring(++index));
                while (index < specification.length() && asciiDigit(specification.charAt(index))) {
                    index++;
                }
            }
        }
        if (negative) {
            west = -west;
        }
        boolean daylight = index < specification.length() && specification.charAt(index) != '\0';
        return daylight
            ? new CrtTimeZone(-(long) west, 3600, CrtTimeZone::usDaylight)
            : new CrtTimeZone(-(long) west, 0, date -> false);
    }

    public static CrtTimeZone fromZoneId(ZoneId zone) {
        ZoneRules rules = zone.getRules();
        Instant now = Instant.now();
        int standard = rules.getStandardOffset(now).getTotalSeconds();
        List<ZoneOffsetTransitionRule> recurring = rules.getTransitionRules();
        ZoneOffsetTransitionRule start = null;
        ZoneOffsetTransitionRule end = null;
        int adjustment = 0;
        for (ZoneOffsetTransitionRule rule : recurring) {
            if (rule.getOffsetBefore().getTotalSeconds() == standard
                    && rule.getOffsetAfter().getTotalSeconds() > standard) {
                start = rule;
                adjustment = rule.getOffsetAfter().getTotalSeconds() - standard;
            } else if (rule.getOffsetAfter().getTotalSeconds() == standard
                    && rule.getOffsetBefore().getTotalSeconds() > standard) {
                end = rule;
            }
        }
        if (start != null && end != null) {
            ZoneOffsetTransitionRule startRule = start;
            ZoneOffsetTransitionRule endRule = end;
            return new CrtTimeZone(standard, adjustment, date -> between(date,
                startRule.createTransition(date.year()).getInstant().getEpochSecond() + standard,
                endRule.createTransition(date.year()).getInstant().getEpochSecond() + standard));
        }

        int year = now.atZone(zone).getYear();
        Instant first = LocalDate.of(year, 1, 1).atStartOfDay(zone).toInstant();
        Instant last = LocalDate.of(year + 1, 1, 1).atStartOfDay(zone).toInstant();
        AnnualTransition annualStart = null;
        AnnualTransition annualEnd = null;
        ZoneOffsetTransition transition = rules.nextTransition(first.minusSeconds(1));
        while (transition != null && transition.getInstant().isBefore(last)) {
            int before = transition.getOffsetBefore().getTotalSeconds();
            int after = transition.getOffsetAfter().getTotalSeconds();
            if (before == standard && after > standard) {
                annualStart = AnnualTransition.of(transition);
                adjustment = after - standard;
            } else if (after == standard && before > standard) {
                annualEnd = AnnualTransition.of(transition);
            }
            transition = rules.nextTransition(transition.getInstant());
        }
        if (annualStart != null && annualEnd != null) {
            AnnualTransition startPattern = annualStart;
            AnnualTransition endPattern = annualEnd;
            int saving = adjustment;
            return new CrtTimeZone(standard, saving, date -> between(date,
                startPattern.seconds(date.year()), endPattern.seconds(date.year()) - saving));
        }
        return fixedOffset(standard);
    }

    static CrtTimeZone systemDefault() {
        String configured = System.getProperty("otland.timezone");
        if (configured != null && !configured.isEmpty()) {
            return of(configured);
        }
        String environment = System.getenv("TZ");
        return environment == null || environment.isEmpty()
            ? fromZoneId(ZoneId.systemDefault()) : of(environment);
    }

    long standardOffset() {
        return standardOffset;
    }

    int daylightAdjustment() {
        return daylightAdjustment;
    }

    boolean isDaylight(CalendarDateTime date) {
        return daylightAdjustment != 0 && daylightRule.matches(date);
    }

    private static boolean usDaylight(CalendarDateTime date) {
        boolean modern = date.year() >= 2007;
        int startMonth = modern ? 3 : 4;
        int endMonth = modern ? 11 : 10;
        int startDay = weekdayInMonth(date.year(), startMonth, modern ? 2 : 1, 0);
        int endDay = weekdayInMonth(date.year(), endMonth, modern ? 1 : 5, 0);
        long start = CrtRuntime.localSeconds(new CalendarDateTime(date.year(), startMonth, startDay, 2, 0, 0));
        long end = CrtRuntime.localSeconds(new CalendarDateTime(date.year(), endMonth, endDay, 1, 0, 0));
        return between(date, start, end);
    }

    private static boolean between(CalendarDateTime date, long start, long end) {
        long value = CrtRuntime.localSeconds(date);
        return start < end ? value >= start && value < end : value >= start || value < end;
    }

    private static int weekdayInMonth(int year, int month, int week, int weekday) {
        long first = CrtRuntime.localSeconds(new CalendarDateTime(year, month, 1, 0, 0, 0)) / 86400;
        int day = 1 + Math.floorMod(weekday - (int) Math.floorMod(first + 4, 7), 7) + (week - 1) * 7;
        if (day > CrtRuntime.monthLength(year, month)) {
            day -= 7;
        }
        return day;
    }

    private static boolean asciiDigit(char value) {
        return value >= '0' && value <= '9';
    }

    @FunctionalInterface
    private interface DaylightRule {
        boolean matches(CalendarDateTime date);
    }

    private record AnnualTransition(int month, int week, int weekday, int secondOfDay) {
        static AnnualTransition of(ZoneOffsetTransition transition) {
            var date = transition.getDateTimeBefore();
            int week = date.getDayOfMonth() + 7 > date.toLocalDate().lengthOfMonth()
                ? 5 : (date.getDayOfMonth() - 1) / 7 + 1;
            return new AnnualTransition(date.getMonthValue(), week, date.getDayOfWeek().getValue() % 7,
                date.toLocalTime().toSecondOfDay());
        }

        long seconds(int year) {
            int day = weekdayInMonth(year, month, week, weekday);
            return CrtRuntime.localSeconds(new CalendarDateTime(year, month, day, 0, 0, secondOfDay));
        }
    }
}
