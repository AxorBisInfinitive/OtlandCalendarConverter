# OtlandCalendarConverter

A cross-platform command-line calendar converter for JDK 25. The package is `com.inf.otland.cc`.

## Build and test

```powershell
mvn verify
```

JUnit Jupiter 5 runs the tests. The executable JAR has no third-party runtime dependencies.

## Run

```powershell
java -jar target/occ-0.1.0.jar --timestamp 0
java -jar target/occ-0.1.0.jar --otland 2196 1 1 0 0 0
java -jar target/occ-0.1.0.jar --gregorian 2026 10 28 12 34 56
java -jar target/occ-0.1.0.jar --now
java -jar target/occ-0.1.0.jar --help
```

Gregorian and Otland dates use `yyyy-MM-dd HH:mm:ss`, for example `2196-01-01 00:00:00`.
Unix timestamps remain decimal seconds. Invalid commands return exit code 2.

The application uses only standard Java. Windows, Linux, and macOS use the same calendar
and number parsing code. No native library, platform-specific executable, or native-access
JVM option is needed.

## Time zones

Time zone selection uses `otland.timezone`, then the `TZ` environment variable, then the
JVM's default zone. Use an explicit setting for consistent results across machines:

```powershell
java -Dotland.timezone=CST-8 -jar target/occ-0.1.0.jar --now
java -Dotland.timezone=Asia/Shanghai -jar target/occ-0.1.0.jar --timestamp 0
```

CRT specifications use a three-character standard name, a signed offset west of UTC,
optional minutes and seconds, and an optional daylight name. For example, `UTC0`, `CST-8`,
`NPT-5:45`, and `PST8PDT` are supported. An explicit daylight suffix adds one hour using
the first Sunday in April through the last Sunday in October before 2007, and the second
Sunday in March through the first Sunday in November from 2007 onward. The ending
transition is evaluated in standard local time.

IANA names such as `Asia/Shanghai` and `America/New_York` are also supported. Their current
standard offset and recurring daylight transitions come from the JDK time zone database.
These annual rules apply to every converted year, rather than applying historical IANA
offset changes. If a zone has no recurring rules, transitions in the current year provide
the annual pattern; a zone with no current transitions uses its current standard offset.
Windows registry time zone data is not bundled. Changes or differences between registry
and JDK time zone definitions can affect named or default zones. Fixed offsets and explicit
CRT specifications provide platform-independent rules.

## Calendar behavior

- One minute has 64 seconds, one hour has 64 minutes and one day has 32 hours.
- A year has 16 months and 512 days. Years divisible by 8 have 513 days.
- Year zero is accepted. Input years range from 0 to 99999.
- Date fields contain ASCII digits. The year uses up to 11 UTF-16 code units and other
  fields use up to 3. Empty year and time fields parse as zero.
- Timestamp input uses up to 19 UTF-16 code units and accepts a numeric prefix. Parsing
  recognizes CRT wide-character digit ranges and whitespace, and saturates on overflow.
- Integer division truncates toward zero. Arithmetic uses signed 32-bit fields and
  signed 64-bit timestamps with wraparound.
- The date-to-seconds calculation adds the supplied day directly. An internal value
  of zero produces year 1, month 1, day 0. A leap year's final day converts back to
  the following year's month 1, day 0.
- Gregorian input uses local standard time (`tm_isdst = 0`). Gregorian output uses
  the selected zone's daylight-saving rules.
- Gregorian timestamps range from 0 through 32536799999 inclusive, ending at
  `3001-01-19 07:59:59 UTC`. Date normalization and the special handling of the first
  and last three representable days are preserved.
- Failed Gregorian input conversion returns timestamp -1. Failed output conversion
  yields `1899 0 -1 -1 -1 -1`. These values continue through the conversion pipeline.

`OtlandCalendar.setInternal()` consumes its intermediate field while splitting a date.
Consequently, `getTime()` reads the remaining day count afterward. `CalendarConverter`
provides the complete conversions and preserves the supplied date on the input side.

`CrtRuntime` implements numeric parsing, Gregorian arithmetic, range checks, date normalization,
and current Unix time. `CrtTimeZone` supplies fixed offsets and annual daylight rules.

All tests run on every supported platform. Test resources cover Otland dates, signed integer
limits, Gregorian boundaries and daylight transitions in ten CRT time zones. Numeric tests
include every UTF-16 prefix and long, mixed-script decimal inputs. Surefire starts with
`TZ=UTC0` so default-zone tests do not depend on the machine's local configuration.
