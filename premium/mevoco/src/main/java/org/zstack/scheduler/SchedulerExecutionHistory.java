package org.zstack.scheduler;

import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.scheduler.SchedulerJobHistoryVO;
import org.zstack.header.scheduler.SchedulerJobHistoryVO_;
import org.zstack.utils.DateCountCache;
import org.zstack.utils.TimeUtils;

import javax.persistence.Query;
import javax.persistence.Tuple;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.*;

import static org.zstack.core.Platform.operr;

/**
 * Created by MaJin on 2020/3/30.
 */

public class SchedulerExecutionHistory {
    private final DatabaseFacade dbf;

    private final DateCountCache successRecords;
    private final DateCountCache failureRecords;
    private final DateCountCache partialSuccessRecords;
    private final DateCountCache runningRecords;

    private final Set<String> jobTypes = new HashSet<>();

    private Calendar transientRecordTime;

    private final List<DateCountCache> allRecords;

    SchedulerExecutionHistory(Collection<String> jobTypes) {
        Timestamp startTime = Q.New(SchedulerJobHistoryVO.class)
                .select(SchedulerJobHistoryVO_.startTime)
                .in(SchedulerJobHistoryVO_.jobType, jobTypes)
                .orderBy(SchedulerJobHistoryVO_.id, SimpleQuery.Od.ASC)
                .limit(1).findValue();

        long now = System.currentTimeMillis();
        Calendar time = Calendar.getInstance();
        time.setTimeInMillis(startTime != null ? startTime.getTime() : now);
        int startYear = time.get(Calendar.YEAR);
        time.setTimeInMillis(now);
        int endYear = time.get(Calendar.YEAR);

        this.successRecords = new DateCountCache(startYear, endYear);
        this.failureRecords = new DateCountCache(startYear, endYear);
        this.partialSuccessRecords = new DateCountCache(startYear, endYear);
        this.runningRecords = new DateCountCache(startYear, endYear);
        this.jobTypes.addAll(jobTypes);
        this.allRecords = Arrays.asList(successRecords, failureRecords, partialSuccessRecords, runningRecords);
        this.dbf = Platform.getComponentLoader().getComponent(DatabaseFacade.class);
    }

    synchronized void fillReport(SchedulerExecutionReport report) {
        if (report.getTimeUnit() == Calendar.HOUR_OF_DAY) {
            queryToFillReport(report);
            return;
        }

        refreshUntil(report.getNowTimeInMiLls());

        int[] succRecords = successRecords.getCounts(report.getStartTime(), report.getTimeUnit(), report.getRange());
        int[] failRecords = failureRecords.getCounts(report.getStartTime(), report.getTimeUnit(), report.getRange());
        int[] partSuccRecords = partialSuccessRecords.getCounts(report.getStartTime(), report.getTimeUnit(), report.getRange());
        int[] runRecords = runningRecords.getCounts(report.getStartTime(), report.getTimeUnit(), report.getRange());

        for (int i = 0; i < report.getRange(); i++) {
            report.getSuccessRecords()[i] += succRecords[i];
            report.getFailureRecords()[i] += failRecords[i];
            report.getPartialSuccessRecords()[i] += partSuccRecords[i];
            report.getWaitingRecords()[i] += runRecords[i];
        }
    }

    private void queryToFillReport(SchedulerExecutionReport report) {
        long endTime = report.getEndTime().getTimeInMillis();
        long startTime = report.getStartTime().getTimeInMillis();
        long timeUnitInMills = (endTime - startTime) / report.getRange();
        String appendSql = String.format(" and h.startTime >= '%s' and h.startTime < '%s'", new Timestamp(startTime), new Timestamp(endTime));

        long count = getHistoryCount(appendSql);
        if (count == 0) {
            return;
        }

        for (int page = 0; page <= (count - 1) / 1000; page++) {
            queryHistory(appendSql, page).forEach(it -> {
                // only support day or hour.
                int index = (int) ((it.get(0, Date.class).getTime() - startTime) / timeUnitInMills);
                addRecordToReport(report, index,
                        !it.get(1, Boolean.class),
                        it.get(2, Boolean.class),
                        it.get(3, Long.class) == -1);
            });
        }
    }

    private void refreshUntil(long timeInMills) {
        Calendar time = Calendar.getInstance();
        time.setTimeInMillis(timeInMills);
        expandCacheIfNeed(time.get(Calendar.YEAR));

        String appendSql;
        if (transientRecordTime == null) {
            appendSql = "";
        } else {
            appendSql = String.format(" and h.startTime >= '%s'", new Timestamp(transientRecordTime.getTimeInMillis()));
            clearRecords(transientRecordTime, time);
        }

        this.transientRecordTime = TimeUtils.roundOff(timeInMills, Calendar.DATE);
        long count = getHistoryCount(appendSql);
        if (count == 0) {
            return;
        }

        for (int page = 0; page <= (count - 1) / 1000; page++) {
            queryHistory(appendSql, page).forEach(it -> addRecordToCache(
                    it.get(0, Date.class),
                    !it.get(1, Boolean.class),
                    it.get(2, Boolean.class),
                    it.get(3, Long.class) == -1));
        }
    }

    private void clearRecords(Calendar from, Calendar to) {
        do {
            for (DateCountCache cache : allRecords) {
                cache.setCountUnsafe(from.get(Calendar.YEAR), from.get(Calendar.MONTH), from.get(Calendar.DAY_OF_MONTH), 0);
            }

            from.add(Calendar.DATE, 1);
        } while (from.before(to));
    }

    private void expandCacheIfNeed(int nowYear) {
        for (DateCountCache record : allRecords) {
            record.expandIfNeed(null, nowYear);
        }
    }

    private void addRecordToReport(SchedulerExecutionReport report, int index, boolean allFailure, boolean allSuccess, boolean running) {
        if (running) {
            report.getWaitingRecords()[index] += 1;
        } else if (allSuccess) {
            report.getSuccessRecords()[index] += 1;
        } else if (allFailure) {
            report.getFailureRecords()[index] += 1;
        } else {
            report.getPartialSuccessRecords()[index] += 1;
        }
    }

    private void addRecordToCache(Date timestamp, boolean allFailure, boolean allSuccess, boolean running) {
        Calendar time = Calendar.getInstance();
        time.setTime(timestamp);

        if (running) {
            if (time.before(this.transientRecordTime)) {
                this.transientRecordTime = TimeUtils.roundOff(time, Calendar.DATE);
            }
            runningRecords.addCountUnsafe(time.get(Calendar.YEAR), time.get(Calendar.MONTH), time.get(Calendar.DAY_OF_MONTH));
        } else if (allSuccess) {
            successRecords.addCountUnsafe(time.get(Calendar.YEAR), time.get(Calendar.MONTH), time.get(Calendar.DAY_OF_MONTH));
        } else if (allFailure) {
            failureRecords.addCountUnsafe(time.get(Calendar.YEAR), time.get(Calendar.MONTH), time.get(Calendar.DAY_OF_MONTH));
        } else {
            partialSuccessRecords.addCountUnsafe(time.get(Calendar.YEAR), time.get(Calendar.MONTH), time.get(Calendar.DAY_OF_MONTH));
        }
    }

    private long getHistoryCount(String appendSql) {
        return SQL.New("select count(distinct h.fireInstanceId)" +
                " from SchedulerJobHistoryVO h" +
                " where h.jobType in :jobTypes" +
                appendSql, Long.class)
                .param("jobTypes", jobTypes).find();
    }

    /**
     *
     * @param appendSql
     * @param page
     * @return startTime, has success, allSuccess, min executeTime
     */

    private List<Tuple> queryHistory(String appendSql, int page) {
        String sql = "SELECT h.startTime, max(h.success), min(h.success), min(h.executeTime)" +
                " FROM SchedulerJobHistoryVO h" +
                " where h.jobType in :jobTypes" +
                appendSql +
                " group by h.fireInstanceId";

        return SQL.New(sql, Tuple.class).limit(1000).offset(page * 1000).param("jobTypes", jobTypes).list();
    }
}
