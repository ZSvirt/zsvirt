package org.zstack.core.cloudbus;

import java.util.List;
import java.util.Map;

/**
 */
public interface CloudBusMXBean {
    Map<String, MessageStatistic> getStatistics();

    List<WaitingReplyMessageStatistic> getWaitingReplyMessageStatistic();

    WaitingMessageSummaryStatistic getWaitingReplyMessageSummaryStatistic();
}
