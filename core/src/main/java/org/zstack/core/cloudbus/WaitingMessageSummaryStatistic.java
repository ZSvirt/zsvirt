package org.zstack.core.cloudbus;

import java.beans.ConstructorProperties;
import java.util.Map;

/**
 */
public class WaitingMessageSummaryStatistic {
    private int totalWaitingMessageNum;
    private Map<String, Integer> waitingMessageCount;
    private String theMostWaitingMessageName;
    private long theMostWaitingMessageNum;
    private String theLongestWaitingTimeMessageName;
    private long theLongestWaitingTime;

    @ConstructorProperties({"totalWaitingMessageNum", "waitingMessageCount", "theMostWaitingMessageName", "theMostWaitingMessageNum",  "theLongestWaitingTimeMessageName", "theLongestWaitingTime"})
    public WaitingMessageSummaryStatistic(int totalWaitingMessageNum, Map<String, Integer> waitingMessageCount, String theMostWaitingMessageName, long theMostWaitingMessageNum, String theLongestWaitingTimeMessageName, long theLongestWaitingTime) {
        this.totalWaitingMessageNum = totalWaitingMessageNum;
        this.waitingMessageCount = waitingMessageCount;
        this.theMostWaitingMessageName = theMostWaitingMessageName;
        this.theMostWaitingMessageNum = theMostWaitingMessageNum;
        this.theLongestWaitingTimeMessageName = theLongestWaitingTimeMessageName;
        this.theLongestWaitingTime = theLongestWaitingTime;
    }

    public long getTheMostWaitingMessageNum() {
        return theMostWaitingMessageNum;
    }

    public long getTheLongestWaitingTime() {
        return theLongestWaitingTime;
    }

    public int getTotalWaitingMessageNum() {
        return totalWaitingMessageNum;
    }

    public Map<String, Integer> getWaitingMessageCount() {
        return waitingMessageCount;
    }

    public String getTheMostWaitingMessageName() {
        return theMostWaitingMessageName;
    }

    public String getTheLongestWaitingTimeMessageName() {
        return theLongestWaitingTimeMessageName;
    }
}
