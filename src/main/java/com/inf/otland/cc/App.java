package com.inf.otland.cc;

import java.io.PrintStream;
import java.util.Arrays;

public final class App {
    private static final String USAGE = """
        Usage:
          java -jar occ-0.1.0.jar --timestamp VALUE
          java -jar occ-0.1.0.jar --otland YEAR MONTH DAY HOUR MINUTE SECOND
          java -jar occ-0.1.0.jar --gregorian YEAR MONTH DAY HOUR MINUTE SECOND
          java -jar occ-0.1.0.jar --now
          java -jar occ-0.1.0.jar --help

        Dates use the configured local time zone. Date fields accept ASCII digits only.
        Use -Dotland.timezone=CST-8 or -Dotland.timezone=Asia/Shanghai to select a time zone.
        """;

    private App() {
    }

    public static void main(String[] args) {
        int status = run(args, System.out, System.err);
        if (status != 0) {
            System.exit(status);
        }
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length == 1 && (args[0].equals("--help") || args[0].equals("-h"))) {
            out.print(USAGE);
            return 0;
        }
        try {
            if (args.length == 0) {
                throw new IllegalArgumentException("A command is required.");
            }
            CalendarConverter.Conversion result = switch (args[0]) {
                case "--timestamp" -> {
                    requireLength(args, 2);
                    yield CalendarConverter.fromTimestampText(args[1]);
                }
                case "--otland" -> {
                    requireLength(args, 7);
                    yield CalendarConverter.fromOtlandFields(Arrays.copyOfRange(args, 1, 7));
                }
                case "--gregorian" -> {
                    requireLength(args, 7);
                    yield CalendarConverter.fromGregorianFields(Arrays.copyOfRange(args, 1, 7));
                }
                case "--now" -> {
                    requireLength(args, 1);
                    yield CalendarConverter.now();
                }
                default -> throw new IllegalArgumentException("Unknown command: " + args[0]);
            };
            out.println("Timestamp: " + result.timestamp());
            out.println("Gregorian: " + result.gregorian().format());
            out.println("Otland: " + result.otland().format());
            return 0;
        } catch (RuntimeException exception) {
            err.println("Error: " + exception.getMessage());
            err.print(USAGE);
            return 2;
        }
    }

    private static void requireLength(String[] args, int expected) {
        if (args.length != expected) {
            throw new IllegalArgumentException("Incorrect number of arguments.");
        }
    }
}
