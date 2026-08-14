package org.zstack.core.thread;

public abstract class AbstractTimeStatisticFuture<T> extends AbstractFuture<T> {
    public AbstractTimeStatisticFuture(AbstractChainTask task) {
        super(task);
    }

    AbstractChainTask getTask() {
        return (AbstractChainTask) task;
    }

    private long startPendingTimeInMills = System.currentTimeMillis();
    private Long startExecutionTimeInMills;

    public long getStartPendingTimeInMills() {
        return startPendingTimeInMills;
    }

    public void setStartPendingTimeInMills(long startPendingTimeInMills) {
        this.startPendingTimeInMills = startPendingTimeInMills;
    }

    public Long getStartExecutionTimeInMills() {
        return startExecutionTimeInMills;
    }

    public void setStartExecutionTimeInMills(Long startExecutionTimeInMills) {
        this.startExecutionTimeInMills = startExecutionTimeInMills;
    }
}
