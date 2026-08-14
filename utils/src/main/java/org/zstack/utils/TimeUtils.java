package org.zstack.utils;

import org.zstack.utils.data.NumberUtils;
import org.zstack.utils.data.UnitNumber;
import org.zstack.utils.logging.CLogger;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 */
public class TimeUtils {

    public static String TIME_UNIT_WEEKS = "WEEKS";
    public static String TIME_UNIT_MONTHS = "MONTHS";

    public static final String[] patterns = {
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ssX",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSX"
    };

    private static final DateTimeFormatter basicFormatter;

    static {
        basicFormatter = new DateTimeFormatterBuilder()
                // Allowed:     "Jan" or "JAN" / "AM" or "am"
                .parseCaseInsensitive()
                // Allowed:     "Thu, 26 Oct 2023 10:30:00 GMT"
                // Not Allowed: "Thu, 26 Oct 2023 10:30:00 Asia/Shanghai"  see more in ZSV-9028
                .appendOptional(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH))
                // Allowed:     "Thu, 5 Jun 2025 15:47:29 +0800"
                .appendOptional(DateTimeFormatter.RFC_1123_DATE_TIME)
                // Allowed:     "2023-10-26T10:30:00+08:00"
                .appendOptional(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                // Allowed:     "2023-10-26T10:30:00.123+08"
                .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSxx", Locale.ENGLISH))
                // Allowed:     "2023-10-26T10:30:00.123+0800"
                .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ENGLISH))
                // Allowed:     "2023-10-26T10:30:00+08:00"
                .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ENGLISH))
                // Allowed:     "2023-10-26T10:30:00+08"
                .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxx", Locale.ENGLISH))
                // Allowed:     "2023-10-26T10:30:00+0800"
                .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH))
                .toFormatter(Locale.ENGLISH);
    }

    private static CLogger logger = Utils.getLogger(TimeUtils.class);

    public static boolean loopExecuteUntilTimeoutIgnoreExceptionAndReturn(long period, long interval, TimeUnit unit, Callable<Boolean> runnable) {
        long count = 0;
        while (count < period) {
            try {
                if (runnable.call()) {
                    return true;
                }

                unit.sleep(interval);
            } catch (Throwable t) {
                logger.debug(String.format("%s, after %s ms timeout", t.getMessage(), period-count));
                Thread.currentThread().interrupt();
            }
            count += interval;
        }

        return false;
    }

    public static void loopExecuteUntilTimeoutIgnoreException(long period, long interval, TimeUnit unit, Callable<Boolean> runnable) {
        if (!loopExecuteUntilTimeoutIgnoreExceptionAndReturn(period, interval, unit, runnable)) {
            throw new RuntimeException(String.format("timeout after %s seconds", period));
        }
    }

    public static boolean isValidTimeFormat(String time) {
        try {
            parseTimeInMillis(time);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static long parseTimeInMillis(String time) {
        UnitNumber numeric = NumberUtils.ofUnitNumberOrThrow(time);
        if ("".equals(numeric.units)) {
            return numeric.number;
        }

        switch (numeric.units) {
        case "s": case "S":
            return TimeUnit.SECONDS.toMillis(numeric.number);
        case "m": case "M":
            return TimeUnit.MINUTES.toMillis(numeric.number);
        case "h": case "H":
            return TimeUnit.HOURS.toMillis(numeric.number);
        case "d": case "D":
            return TimeUnit.DAYS.toMillis(numeric.number);
        case "w": case "W":
            return TimeUnit.DAYS.toMillis(numeric.number * 7);
        case "y": case "Y":
            return TimeUnit.DAYS.toMillis(numeric.number * 365);
        }
        throw new NumberFormatException("unsupported time format: " + time);
    }

    public static long parseTimeToSeconds(String time){
        if (time.equals(TimeUtils.TIME_UNIT_WEEKS)) {
            return TimeUnit.DAYS.toSeconds(7);
        }
        if (time.equals(TimeUtils.TIME_UNIT_MONTHS)) {
            return TimeUnit.DAYS.toSeconds(30);
        }
        return TimeUnit.valueOf(time).toSeconds(1);
    }

    public static long parseTimeToMillis(String time){
        if (time.equals(TimeUtils.TIME_UNIT_WEEKS)) {
            return TimeUnit.DAYS.toMillis(7);
        }
        if (time.equals(TimeUtils.TIME_UNIT_MONTHS)) {
            return TimeUnit.DAYS.toMillis(30);
        }
        return TimeUnit.valueOf(time).toMillis(1);
    }

    private static final String DEFAULT_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static boolean isValidTimestampFormat(String timestamp) {
        try {
            new SimpleDateFormat(DEFAULT_TIME_FORMAT).parse(timestamp);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    public static boolean isValidTimestampFormat(String timestamp, String dformat) {
        try {
            DateFormat dateFormat = new SimpleDateFormat(dformat);
            dateFormat.parse(timestamp);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    public static long parseFormatStringToTimeStamp(String timestamp, String dformat) {
        try {
            DateFormat dateFormat = new SimpleDateFormat(dformat);
            return dateFormat.parse(timestamp).getTime();
        } catch (ParseException e) {
            return 0;
        }
    }

    public static long parseFormatStringToTimeStamp(String timestamp) {
        try {
            return new SimpleDateFormat(DEFAULT_TIME_FORMAT).parse(timestamp).getTime();
        } catch (ParseException e) {
            return 0;
        }
    }

    public static String getCurrentTimeStamp() {
        return instantToString(Instant.now());
    }

    public static String getCurrentTimeStamp(String dformat) {
        return instantToString(Instant.now(), dformat);
    }

    public static Calendar roundOff(long timeInMills, int timeUnit) {
        Calendar time = Calendar.getInstance();
        time.setTimeInMillis(timeInMills);
        for (int i = timeUnit + 1; i <= Calendar.MILLISECOND; i++) {
            time.clear(i);
        }

        return time;
    }

    public static Calendar roundOff(Calendar time, int timeUnit) {
        time = (Calendar) time.clone();
        for (int i = timeUnit + 1; i <= Calendar.MILLISECOND; i++) {
            time.clear(i);
        }
        return time;
    }

    public static Calendar roundUp(Calendar time, int timeUnit) {
        time = (Calendar) time.clone();
        for (int i = timeUnit + 1; i <= Calendar.MILLISECOND; i++) {
            time.clear(i);
        }
        time.add(timeUnit, 1);
        return time;
    }

    public static boolean equalApproximately(Calendar c1, Calendar c2, int roundOffTimeUnit) {
        for (int i = roundOffTimeUnit; i >= Calendar.YEAR; i--) {
            if (c1.get(i) != c2.get(i)) {
                return false;
            }
        }
        return true;
    }

    public static String parseRFC3339DateTime(String time) {
        for (String pattern : patterns) {
            try {
                SimpleDateFormat df = new SimpleDateFormat(pattern);
                df.setTimeZone(TimeZone.getTimeZone("UTC"));
                return df.format(df.parse(time));
            } catch (ParseException e) {
                continue;
            }
        }

        return null;
    }

    public static String instantToString(Instant instant) {
        return instantToString(instant, DEFAULT_TIME_FORMAT);
    }

    public static String instantToString(Instant instant, String format) {
        return DateTimeFormatter.ofPattern(format).withZone(ZoneId.systemDefault()).format(instant);
    }

    /**
     * @param timestamp using milliseconds from the epoch of 1970-01-01T00:00:00Z
     */
    public static LocalDateTime toLocalDateTime(long timestamp) {
        return toLocalDateTime(Instant.ofEpochMilli(timestamp));
    }

    public static LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    public static OffsetDateTime offsetDateTimeOf(long timestampInMilli) {
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(timestampInMilli), ZoneId.systemDefault());
    }

    public static ZonedDateTime parseZonedDateTimeByBasicFormatter(String text) {
        return ZonedDateTime.parse(text, basicFormatter);
    }
}
