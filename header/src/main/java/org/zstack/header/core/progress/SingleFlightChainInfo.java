package org.zstack.header.core.progress;

import java.util.ArrayList;
import java.util.List;

public class SingleFlightChainInfo {
    private List<RunningTaskInfo> runningTask = new ArrayList<>();
    private List<PendingTaskInfo> pendingTask = new ArrayList<>();

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
}
