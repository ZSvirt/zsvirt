package org.zstack.zql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class ZQLStatistic {
    private List<SlowZQLStatistic> slowZQLStatistics = Collections.synchronizedList(new ArrayList<>());
    private AtomicLong executedZqlCount = new AtomicLong(0);

    public List<SlowZQLStatistic> getSlowZQLStatistics() {
        return slowZQLStatistics;
    }

    public void setSlowZQLStatistics(List<SlowZQLStatistic> slowZQLStatistics) {
        this.slowZQLStatistics = slowZQLStatistics;
    }

    public AtomicLong getExecutedZqlCount() {
        return executedZqlCount;
    }

    public void setExecutedZqlCount(AtomicLong executedZqlCount) {
        this.executedZqlCount = executedZqlCount;
    }

    public void count() {
        executedZqlCount.incrementAndGet();
    }

    public void resetCount() {
        executedZqlCount.set(0);
    }
}
