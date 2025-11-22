package violet.aigc.common.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TimeUtil {

    private static final DateTimeFormatter YYYYMMDD_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static String getNowDate() {
        LocalDate currentDate = LocalDate.now();
        return currentDate.format(YYYYMMDD_FORMATTER);
    }

    public static List<String> getLast7DaysList() {
        List<String> dateList = new ArrayList<>(7);
        LocalDate currentDate = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate targetDate = currentDate.minusDays(i);
            String formattedDate = targetDate.format(YYYYMMDD_FORMATTER);
            dateList.add(formattedDate);
        }
        return dateList;
    }
}
