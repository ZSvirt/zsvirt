package org.zstack.core.thread;

import java.util.Map;

/**
 */
public interface ThreadFacadeMXBean {
    Map<String, SyncTaskStatistic> getSyncTaskStatistics();

    Map<String, ChainTaskStatistic> getChainTaskStatistics();

    ThreadPoolStatistic getThreadPoolStatistic();
}
