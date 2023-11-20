package za.co.mawa.bes.utils;

import java.util.Date;

public class DateTime {

    public static Date resetTime(Date date) {
        date.setHours(0);
        date.setMinutes(0);
        date.setSeconds(0);
        date.setTime(date.getTime() - (date.getTime() % 1000)); // Clear milliseconds
        return date;
    }
}
