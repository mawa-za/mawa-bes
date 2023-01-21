package za.co.mawa.bes.utils;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Conversion {
    private static final DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
    private static final DateFormat dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final DateFormat TimeFormatter = new SimpleDateFormat("HH:mm:ss");
    public static String dateToString(Date date) {
        String stringDate = "";

        if (date != null) {
            stringDate = dateFormatter.format(date);
        }
        return stringDate;
    }

    public static String dateTimeToString(Date date) {
        String stringDate = "";

        if (date != null) {
            stringDate = dateTimeFormatter.format(date);
        }
        return stringDate;
    }
    public static String TimeToString(Date date) {
        String stringDate = "";

        if (date != null) {
            stringDate = TimeFormatter.format(date);
        }
        return stringDate;
    }
    public static Date stringToDate(String date) {
        Date returnDate = null;
        try {
            returnDate = dateFormatter.parse(date);
        } catch (ParseException ex) {
            Logger.getLogger(Conversion.class.getName()).log(Level.SEVERE, null, ex);
        }

        return returnDate;
    }

    public static Date stringToDateTime(String date) {
        Date returnDate = null;
        try {
            returnDate = dateTimeFormatter.parse(date);
        } catch (ParseException ex) {
            Logger.getLogger(Conversion.class.getName()).log(Level.SEVERE, null, ex);
        }

        return returnDate;
    }

    public static Date addMonthsToDate(Date date, Integer months) {

//        String dt = "2008-01-01";  // Start date
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.MONTH, months);  // number of months to add
        return c.getTime();
    }

    public static Date addDaysToDate(Date date, Integer days) {

//        String dt = "2008-01-01";  // Start date
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DATE, days);  // number of days to add
        return c.getTime();
    }

    public static Date addHoursToDate(Date date,Integer hours)
    {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.HOUR_OF_DAY,hours);

        return c.getTime();
    }

    public static BigDecimal stringToBigDecimal(String value) {
        value = value.replace(",", "");
        return new BigDecimal(value);
    }

    public static String stringToBigDecimal(BigDecimal value) {
        return value.toString();
    }

    public static Integer stringToInteger(String value) {
        value = value.replace(",", "");
        return new Integer(value);
    }
}
