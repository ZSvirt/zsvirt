package org.zstack.expon.sdk;

import org.zstack.expon.sdk.volume.VolumeTask;

import java.util.List;

public class ExponTask {
    private String id;
    private String taskId;
    private String name;
    private String description;
    private String operand;
    private long beginTime;
    private long endTime;
    private String status;
    private int progress;
    private String retCode;
    private String retMsg;
    private List<String> steps;
    private int curStep;
    private boolean processing;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOperand() {
        return operand;
    }

    public void setOperand(String operand) {
        this.operand = operand;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }



    public List<String> getSteps() {
        return steps;
    }

    public void setSteps(List<String> steps) {
        this.steps = steps;
    }

    public int getCurStep() {
        return curStep;
    }

    public void setCurStep(int curStep) {
        this.curStep = curStep;
    }

    public boolean isProcessing() {
        return processing;
    }

    public void setProcessing(boolean processing) {
        this.processing = processing;
    }

    public String getRetCode() {
        return retCode;
    }

    public void setRetCode(String retCode) {
        this.retCode = retCode;
    }

    public boolean isSuccess() {
        return "0".equals(retCode);
    }

    public String getRetMsg() {
        return retMsg;
    }

    public void setRetMsg(String retMsg) {
        this.retMsg = retMsg;
    }

    public long getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(long beginTime) {
        this.beginTime = beginTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public static ExponTask valueOf(GetTaskStatusResponse rsp) {
        ExponTask task = new ExponTask();
        task.setId(rsp.getId());
        task.setTaskId(rsp.getTaskId());
        task.setName(rsp.getName());
        task.setDescription(rsp.getDescription());
        task.setOperand(rsp.getOperand());
        task.setBeginTime(rsp.getBeginTime());
        task.setEndTime(rsp.getEndTime());
        task.setStatus(rsp.getStatus());
        task.setProgress(rsp.getProgress());
        task.setRetCode(rsp.getRetCode());
        task.setRetMsg(rsp.getRetMsg());
        task.setCurStep(rsp.getCurStep());
        return task;
    }

    public static ExponTask valueOf(VolumeTask volumeTask) {
        ExponTask task = new ExponTask();
        task.setName(volumeTask.getVolName());
        task.setProgress(volumeTask.getProgress());
        task.setRetCode(Integer.toString(volumeTask.getCode()));
        task.setRetMsg(volumeTask.getMsg());

        switch (volumeTask.getState()) {
            case "TASK_COMPLETE":
                task.setStatus(TaskStatus.SUCCESS.toString());
                break;
            case "TASK_FAILED":
                task.setStatus(TaskStatus.FAILED.toString());
                break;
            case "TASK_STATE_TORUN":
            case "TASK_STATE_RUNNING":
                task.setStatus(TaskStatus.RUNNING.toString());
                break;
            default:
                task.setStatus(TaskStatus.FAILED.toString());
                task.setRetMsg("Unknown task state: " + volumeTask.getState());
                task.setRetCode("1");
                break;
        }
        return task;

    }
}
