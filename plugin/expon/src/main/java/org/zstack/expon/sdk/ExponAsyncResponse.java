package org.zstack.expon.sdk;

public class ExponAsyncResponse extends ExponResponse {
    private String taskId;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
}
