package org.zstack.utils;

import org.zstack.utils.logging.CLogger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 */
public class DebugUtils {
    public static class TimeStatistic {
        private long totalTime;
        private long count;
        private long averageTime;
        private String name;

        public long getTotalTime() {
            return totalTime;
        }

        public void setTotalTime(long totalTime) {
            this.totalTime = totalTime;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

        public long getAverageTime() {
            return averageTime;
        }

        public void setAverageTime(long averageTime) {
            this.averageTime = averageTime;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void addStatistic(long time) {
            totalTime += time;
            count ++;
            averageTime = totalTime / count;
        }

        @Override
        public String toString() {
            return String.format("Time statistic[%s]: total time: %s secs, total count: %s, average time: %s secs",
                    name,
                    TimeUnit.MILLISECONDS.toSeconds(totalTime),
                    count,
                    TimeUnit.MILLISECONDS.toSeconds(averageTime));
        }
    }

    private static final CLogger logger = Utils.getLogger(DebugUtils.class);
    private static final Map<String, TimeStatistic> timeStatistics = new ConcurrentHashMap<String, TimeStatistic>();

    public static String getStackTrace() {
        StackTraceElement[] statements = Thread.currentThread().getStackTrace();
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement s : statements) {
            sb.append("\t" + s.toString() + "\n");
        }

        return sb.toString();
    }

    public static String getStackTrace(Throwable t) {
        StringWriter stringWriter= new StringWriter();
        PrintWriter writer= new PrintWriter(stringWriter);
        t.printStackTrace(writer);
        StringBuffer buffer= stringWriter.getBuffer();
        return buffer.toString();
    }

    public static void dumpStackTrace(String msg) {
        String stack = String.format("%s:\n%s", msg, getStackTrace());
        logger.debug(stack);
    }

    public static void dumpStackTrace() {
        logger.debug(getStackTrace());
    }

    public static void Assert(boolean exp, String msg) {
        if (!exp) {
            throw new RuntimeException(msg);
        }
    }

    private static Throwable recursivelyGetRootCause(Throwable e) {
        if (e.getCause() != null) {
            return recursivelyGetRootCause(e.getCause());
        }
        return e;
    }

    public static void countTimeStatistic(String name, long time) {
        TimeStatistic stat = timeStatistics.get(name);
        if (stat == null) {
            stat = new TimeStatistic();
            stat.setName(name);
            timeStatistics.put(name, stat);
        }
        stat.addStatistic(time);
    }

    public static Map<String, TimeStatistic> getTimeStatistics() {
        return timeStatistics;
    }

    public static Throwable getRootCause(Throwable e) {
        return recursivelyGetRootCause(e);
    }

    public static void dumpAllThreads() {
        final StringBuilder dump = new StringBuilder();
        final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        final ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), 100);
        for (ThreadInfo threadInfo : threadInfos) {
            dump.append('"');
            dump.append(threadInfo.getThreadName());
            dump.append("\" ");
            final Thread.State state = threadInfo.getThreadState();
            dump.append("\n   java.lang.Thread.State: ");
            dump.append(state);
            final StackTraceElement[] stackTraceElements = threadInfo.getStackTrace();
            for (final StackTraceElement stackTraceElement : stackTraceElements) {
                dump.append("\n        at ");
                dump.append(stackTraceElement);
            }
            dump.append("\n\n");
        }

        logger.debug(dump.toString());
    }

    public static void shouldNotBeHere(String msg) {
        if (msg == null) {
            msg = "code should not reach here";
        }

        throw new RuntimeException(msg);
    }
}
