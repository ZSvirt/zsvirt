package org.zstack.expon.sdk;


/** @example
 * {
 * "begin_time": 1689936631393,
 * "cur_step": 1,
 * "description": "",
 * "end_time": 1689936631622,
 * "id": "349a69c9-fa88-48f3-bae7-8e6e0a7f952c",
 * "name": "Delete block storage volume",
 * "operand": "volume:testclone",
 * "private": "",
 * "progress": 100,
 * "ret_code": "0",
 * "ret_msg": "",
 * "status": "SUCCESS",
 * "steps": ["pool delete volume", "database delete volume"],
 * "task_id": "b01.003",
 * "message": ""
 * }
 */
public class GetTaskStatusResponse extends ExponResponse {
    private String id;
    private String taskId;
    private String name;
    private String description;
    private String operand;
    private long beginTime;
    private long endTime;
    private String status;
    private int progress;
    private String retMsg;
    private int curStep;
    private String private_;

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

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    @Override
    public String getRetCode() {
        return retCode;
    }

    @Override
    public void setRetCode(String retCode) {
        this.retCode = retCode;
    }

    public String getRetMsg() {
        return retMsg;
    }

    public void setRetMsg(String retMsg) {
        this.retMsg = retMsg;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public void setMessage(String message) {
        this.message = message;
    }

    public int getCurStep() {
        return curStep;
    }

    public void setCurStep(int curStep) {
        this.curStep = curStep;
    }

    public String getPrivate_() {
        return private_;
    }

    public void setPrivate_(String private_) {
        this.private_ = private_;
    }
}
