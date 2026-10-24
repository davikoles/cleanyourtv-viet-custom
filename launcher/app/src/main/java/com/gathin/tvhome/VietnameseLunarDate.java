package com.gathin.tvhome;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;

/**
 * Vietnamese lunar date for this personal launcher build. No Android APIs, I/O or network.
 * Calendar conversion adapted from Ho Ngoc Duc's amlich-aa98 astronomical algorithm:
 * https://www.informatik.uni-leipzig.de/~duc/amlich/
 * Reference distribution: https://github.com/vanng822/amlich/blob/master/lib/amlich-aa98.js
 *
 * Copyright (c) 2006 Ho Ngoc Duc. All Rights Reserved.
 * Astronomical algorithms from the book "Astronomical Algorithms" by Jean Meeus, 1998.
 * Permission to use, copy, modify, and redistribute this software and its
 * documentation for personal, non-commercial use is hereby granted provided that
 * this copyright notice and appropriate documentation appears in all copies.
 *
 * This Java adaptation keeps that notice and uses the Vietnam UTC+7 civil date,
 * independently of the device's selected time zone. It is not the Shamsi calendar.
 */
final class VietnameseLunarDate {
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final double TIME_ZONE = 7.0;
    private static LocalDate cachedDate;
    private static String cachedText;

    private VietnameseLunarDate() {}

    /** Called on Home resume and the existing minute tick; recalculate only once per civil day. */
    static String today() {
        LocalDate date = LocalDate.now(VIETNAM);
        if (!date.equals(cachedDate)) {
            cachedText = format(date);
            cachedDate = date;
        }
        return cachedText;
    }

    static String format(LocalDate date) {
        int[] lunar = fromSolar(date);
        return String.format(Locale.ROOT, "\u00c2m l\u1ecbch: %02d/%02d/%04d%s",
                lunar[0], lunar[1], lunar[2], lunar[3] == 1 ? " (nhu\u1eadn)" : "");
    }

    /** Result order: lunar day, lunar month, lunar year, leap-month flag (0 or 1). */
    static int[] fromSolar(LocalDate date) {
        int year = date.getYear();
        int dayNumber = julianDay(date.getDayOfMonth(), date.getMonthValue(), year);
        int k = floor((dayNumber - 2415021.076998695) / 29.530588853);
        // The mean-month estimate can be one lunation off near an exact boundary.
        // Bracket the civil date explicitly rather than ever displaying day 0 or 31.
        while (newMoonDay(k) > dayNumber) k--;
        while (newMoonDay(k + 1) <= dayNumber) k++;
        int monthStart = newMoonDay(k);
        int a11 = monthEleven(year);
        int b11 = a11;
        int lunarYear;
        if (a11 >= monthStart) {
            lunarYear = year;
            a11 = monthEleven(year - 1);
        } else {
            lunarYear = year + 1;
            b11 = monthEleven(year + 1);
        }
        int lunarDay = dayNumber - monthStart + 1;
        int diff = floor((monthStart - a11) / 29.0);
        int lunarMonth = diff + 11;
        int leap = 0;
        if (b11 - a11 > 365) {
            int leapOffset = leapMonthOffset(a11);
            if (diff >= leapOffset) {
                lunarMonth = diff + 10;
                if (diff == leapOffset) leap = 1;
            }
        }
        if (lunarMonth > 12) lunarMonth -= 12;
        if (lunarMonth >= 11 && diff < 4) lunarYear--;
        return new int[] { lunarDay, lunarMonth, lunarYear, leap };
    }

    private static int floor(double value) { return (int) Math.floor(value); }

    private static int julianDay(int day, int month, int year) {
        int a = (14 - month) / 12;
        int y = year + 4800 - a;
        int m = month + 12 * a - 3;
        int jd = day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045;
        if (jd < 2299161) jd = day + (153 * m + 2) / 5 + 365 * y + y / 4 - 32083;
        return jd;
    }

    private static int newMoonDay(int k) {
        double t = k / 1236.85;
        double t2 = t * t;
        double t3 = t2 * t;
        double radians = Math.PI / 180.0;
        double jd = 2415020.75933 + 29.53058868 * k + 0.0001178 * t2 - 0.000000155 * t3;
        jd += 0.00033 * Math.sin((166.56 + 132.87 * t - 0.009173 * t2) * radians);
        double m = 359.2242 + 29.10535608 * k - 0.0000333 * t2 - 0.00000347 * t3;
        double moon = 306.0253 + 385.81691806 * k + 0.0107306 * t2 + 0.00001236 * t3;
        double f = 21.2964 + 390.67050646 * k - 0.0016528 * t2 - 0.00000239 * t3;
        double correction = (0.1734 - 0.000393 * t) * Math.sin(m * radians)
                + 0.0021 * Math.sin(2 * m * radians);
        correction += -0.4068 * Math.sin(moon * radians) + 0.0161 * Math.sin(2 * moon * radians);
        correction -= 0.0004 * Math.sin(3 * moon * radians);
        correction += 0.0104 * Math.sin(2 * f * radians) - 0.0051 * Math.sin((m + moon) * radians);
        correction += -0.0074 * Math.sin((m - moon) * radians) + 0.0004 * Math.sin((2 * f + m) * radians);
        correction -= 0.0004 * Math.sin((2 * f - m) * radians) + 0.0006 * Math.sin((2 * f + moon) * radians);
        correction += 0.0010 * Math.sin((2 * f - moon) * radians) + 0.0005 * Math.sin((2 * moon + m) * radians);
        double deltaT = t < -11
                ? 0.001 + 0.000839 * t + 0.0002261 * t2 - 0.00000845 * t3 - 0.000000081 * t * t3
                : -0.000278 + 0.000265 * t + 0.000262 * t2;
        return floor(jd + correction - deltaT + 0.5 + TIME_ZONE / 24.0);
    }

    private static int sunSector(int julianDay) {
        double t = (julianDay - 0.5 - TIME_ZONE / 24.0 - 2451545.0) / 36525.0;
        double t2 = t * t;
        double radians = Math.PI / 180.0;
        double m = 357.52910 + 35999.05030 * t - 0.0001559 * t2 - 0.00000048 * t * t2;
        double longitude = 280.46645 + 36000.76983 * t + 0.0003032 * t2;
        longitude += (1.914600 - 0.004817 * t - 0.000014 * t2) * Math.sin(m * radians)
                + (0.019993 - 0.000101 * t) * Math.sin(2 * m * radians)
                + 0.000290 * Math.sin(3 * m * radians);
        double angle = longitude * radians;
        angle -= 2 * Math.PI * Math.floor(angle / (2 * Math.PI));
        return floor(angle / Math.PI * 6);
    }

    private static int monthEleven(int year) {
        int k = floor((julianDay(31, 12, year) - 2415021.0) / 29.530588853);
        int moon = newMoonDay(k);
        return sunSector(moon) >= 9 ? newMoonDay(k - 1) : moon;
    }

    private static int leapMonthOffset(int a11) {
        int k = floor((a11 - 2415021.076998695) / 29.530588853 + 0.5);
        int i = 1;
        int sector = sunSector(newMoonDay(k + i));
        int previous;
        do {
            previous = sector;
            sector = sunSector(newMoonDay(k + ++i));
        } while (sector != previous && i < 14);
        return i - 1;
    }
}
