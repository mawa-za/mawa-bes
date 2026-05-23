package za.co.mawa.bes.service.v2;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public final class PeriodUtil {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private PeriodUtil() {
    }

    public static String currentPeriod() {
        return YearMonth.now().format(FORMATTER);
    }

    public static String nextPeriod(String periodYYYYMM) {
        YearMonth ym = YearMonth.parse(periodYYYYMM, FORMATTER);
        return ym.plusMonths(1).format(FORMATTER);
    }

    public static boolean isValidPeriod(String periodYYYYMM) {
        if (periodYYYYMM == null || !periodYYYYMM.matches("\\d{6}")) {
            return false;
        }

        try {
            YearMonth.parse(periodYYYYMM, FORMATTER);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public static boolean isNextPeriod(String currentPeriodYYYYMM, String nextPeriodYYYYMM) {
        return nextPeriod(currentPeriodYYYYMM).equals(nextPeriodYYYYMM);
    }
}