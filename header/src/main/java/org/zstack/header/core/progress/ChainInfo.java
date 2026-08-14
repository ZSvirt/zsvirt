package org.zstack.header.core.progress;

import org.zstack.header.rest.APINoSee;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by MaJin on 2019/7/3.
 */
public class ChainInfo {
    private List<RunningTaskInfo> runningTask = new ArrayList<>();
    private List<PendingTaskInfo> pendingTask = new ArrayList<>();
    @APINoSee
    private long maxThreadNum;

    public void setPendingTask(List<PendingTaskInfo> pendingTask) {
        this.pendingTask = pendingTask;
    }

    public void setRunningTask(List<RunningTaskInfo> runningTask) {
        this.runningTask = runningTask;
    }

    public List<RunningTaskInfo> getRunningTask() {
        return runningTask;
    }

    public List<PendingTaskInfo> getPendingTask() {
        return pendingTask;
    }

    public void addRunningTask(RunningTaskInfo task) {
        this.runningTask.add(task);
    }

    public void addPendingTask(PendingTaskInfo pendingTask) {
        this.pendingTask.add(pendingTask);
    }

    public void setMaxThreadNum(long maxThreadNum) {
        this.maxThreadNum = maxThreadNum;
    }

    public long getMaxThreadNum() {
        return maxThreadNum;
    }

    @Override
    public String toString() {
        StringBuilder tb = new StringBuilder();
        tb.append(String.format("\nRUNNING TASK NUMBER: %s", runningTask.size()));
        tb.append(String.format("\nPENDING TASK NUMBER: %s", pendingTask.size()));
        tb.append(String.format("\nASYNC LEVEL: %s", maxThreadNum));

        runningTask.forEach(tb::append);
        pendingTask.forEach(tb::append);

        return tb.toString();
    }
}
