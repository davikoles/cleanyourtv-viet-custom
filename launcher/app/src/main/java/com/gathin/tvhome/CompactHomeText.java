package com.gathin.tvhome;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Vietnamese header content; independent of device language and no owner name baked into the APK. */
final class CompactHomeText {
    private static final Locale VI = new Locale("vi", "VN");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE, d 'tháng' M", VI);
    private static final String[] CAN = {"Giáp", "Ất", "Bính", "Đinh", "Mậu", "Kỷ", "Canh", "Tân", "Nhâm", "Quý"};
    private static final String[] CHI = {"Tý", "Sửu", "Dần", "Mão", "Thìn", "Tỵ", "Ngọ", "Mùi", "Thân", "Dậu", "Tuất", "Hợi"};
    private CompactHomeText() { }

    static String owner(String value) {
        if (value == null) return "";
        String name = Normalizer.normalize(value, Normalizer.Form.NFC)
                .replaceAll("[\\p{Cntrl}\\p{Cf}\\p{Z}\\s]+", " ").trim();
        if (name.codePointCount(0, name.length()) > 40) name = name.substring(0, name.offsetByCodePoints(0, 40)).trim();
        return name;
    }
    static String greeting(String owner) {
        String name = owner(owner);
        return name.isEmpty() ? "XIN CHÀO" : "XIN CHÀO, " + name.toUpperCase(VI);
    }
    static String date(LocalDate value) { return DATE.format(value).toUpperCase(VI); }
    static String lunar(LocalDate date) {
        int[] value = VietnameseLunarDate.fromSolar(date);
        String year = CAN[Math.floorMod(value[2] + 6, 10)] + " " + CHI[Math.floorMod(value[2] + 8, 12)];
        return (value[0] + "/" + value[1] + (value[3] == 1 ? " NHUẬN" : "") + " ÂM LỊCH · " + year).toUpperCase(VI);
    }
}
