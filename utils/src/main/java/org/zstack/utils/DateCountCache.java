package org.zstack.utils;

import java.util.Calendar;

public class DateCountCache {
    private int startYear;
    private int[][][] counts;
    private int endYear;

    /**
     * call expandIfNeed(Integer newStartYear, Integer newEndYear) first, or you may encounter OutOfIndexException.
     * @param year
     * @param month
     * @param day
     * @param newCount
     */

    public synchronized void setCountUnsafe(int year, int month, int day, int newCount) {
        int originValue = counts[year - startYear][month][day];
        if (originValue != newCount) {
            counts[year - startYear][month][day] = newCount;
            counts[year - startYear][month][0] += newCount - originValue;
        }
    }

    public synchronized void addCountUnsafe(int year, int month, int day) {
        counts[year - startYear][month][day] += 1;
        counts[year - startYear][month][0] += 1;
    }

    /**
     *
     * @param year base 1
     * @param month 0-11, base 0
     * @param day 1-31, base 1
     * @return count on specific date
     */

    public int getCount(int year, int month, int day) {
        if (year > endYear|| year < startYear) {
            return 0;
        }

        return counts[year - startYear][month][day];
    }

    public int getCount(int year, int month) {
        if (year > endYear|| year < startYear) {
            return 0;
        }

        return counts[year - startYear][month][0];
    }

    public DateCountCache(int startYear, int endYear) {
        this.startYear = startYear;
        this.endYear = endYear;
        counts = new int[endYear - startYear + 1][12][31 + 1];
    }

    public void expandIfNeed(Integer newStartYear, Integer newEndYear) {
        newStartYear = newStartYear == null || newStartYear > startYear ? startYear : newStartYear;
        newEndYear = newEndYear == null || newEndYear < endYear ? endYear : newEndYear;
        int[][][] newCounts = new int[newEndYear - newStartYear + 1][12][31 + 1];

        for (int i = startYear - newStartYear; i < newCounts.length - (newEndYear - endYear); i++) {
            newCounts[i] = counts[i - (startYear - newStartYear)];
        }

        startYear = newStartYear;
        endYear = newEndYear;
        counts = newCounts;
    }

    public int sum(Calendar from, Calendar to) {
        int sum = 0;
        from = (Calendar) from.clone();
        while (from.before(to)) {
            sum += getCount(from.get(Calendar.YEAR), from.get(Calendar.MONTH), from.get(Calendar.DAY_OF_MONTH));
            from.add(Calendar.DATE, 1);
        }
        return sum;
    }

    public int[] getCounts(Calendar startTime, int timeUnit, int range) {
        int[] results = new int[range];
        startTime = (Calendar) startTime.clone();
        if (timeUnit == Calendar.MONTH) {
            for (int i = 0; i < range; i++) {
                results[i] = getCount(startTime.get(Calendar.YEAR), startTime.get(Calendar.MONTH));
                startTime.add(Calendar.MONTH, 1);
            }
        } else if (timeUnit == Calendar.DATE) {
            for (int i = 0; i < range; i++) {
                results[i] = getCount(startTime.get(Calendar.YEAR), startTime.get(Calendar.MONTH), startTime.get(Calendar.DAY_OF_MONTH));
                startTime.add(Calendar.DATE, 1);
            }
        } else {
            throw new RuntimeException("time unit only support month or day.");
        }
        return results;
    }

    public int getCount(Calendar time, int roundOffTimeUnit) {
        if (roundOffTimeUnit == Calendar.MONTH) {
            return getCount(time.get(Calendar.YEAR), time.get(Calendar.MONTH));
        } else if (roundOffTimeUnit == Calendar.DATE) {
            return getCount(time.get(Calendar.YEAR), time.get(Calendar.MONTH), time.get(Calendar.DATE));
        } else {
            throw new RuntimeException("time unit only support month or day.");
        }
    }
}
