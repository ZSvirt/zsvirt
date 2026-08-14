package org.zstack.expon.sdk.volume;

/**
 * @example
 * {
 * 	"cloneid": 0,
 * 	"code": 0,
 * 	"dest_vol": "volume-e6a6c343-0eae-4505-be84-022e85dabf83",
 * 	"first_blockid": 0,
 * 	"last_blockid": 255,
 * 	"msg": "",
 * 	"namespace": "ussns",
 * 	"path": "",
 * 	"pool_id": 1,
 * 	"progress": 100,
 * 	"progress_blockid": 233,
 * 	"rw_iops": 0,
 * 	"rw_mbytes": 0,
 * 	"snap_source_vol": "",
 * 	"snapid": 0,
 * 	"speed": 8,
 * 	"state": "TASK_COMPLETE",
 * 	"task_id": 1,
 * 	"task_type": 3,
 * 	"treeid": 2,
 * 	"update_at": 1704699092,
 * 	"uss_id": 1,
 * 	"vol_name": "snapshot-1f0ba3d1-bff4-473b-af71-d1a74788f8a0",
 * 	"vol_size": 1073741824
 * }
 */
public class VolumeTask {
    public enum TaskState {
        TASK_STATE_TORUN,
        TASK_STATE_RUNNING,
        TASK_FAILED,
        TASK_COMPLETE,
    }


    private String volName;
    private long volSize;
    private int cloneid;
    private int code;
    private String destVol;
    private int firstBlockid;
    private int lastBlockid;
    private String msg;
    private String namespace;
    private String path;
    private int poolId;
    private int progress;
    private int progressBlockid;
    private int rwIops;
    private int rwMbytes;
    private String snapSourceVol;
    private int snapid;
    private int speed;
    private String state;
    private int taskId;
    private int taskType;
    private int treeid;
    private long updateAt;
    private int ussId;

    public String getVolName() {
        return volName;
    }

    public void setVolName(String volName) {
        this.volName = volName;
    }

    public long getVolSize() {
        return volSize;
    }

    public void setVolSize(long volSize) {
        this.volSize = volSize;
    }

    public int getCloneid() {
        return cloneid;
    }

    public void setCloneid(int cloneid) {
        this.cloneid = cloneid;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getDestVol() {
        return destVol;
    }

    public void setDestVol(String destVol) {
        this.destVol = destVol;
    }

    public int getFirstBlockid() {
        return firstBlockid;
    }

    public void setFirstBlockid(int firstBlockid) {
        this.firstBlockid = firstBlockid;
    }

    public int getLastBlockid() {
        return lastBlockid;
    }

    public void setLastBlockid(int lastBlockid) {
        this.lastBlockid = lastBlockid;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getPoolId() {
        return poolId;
    }

    public void setPoolId(int poolId) {
        this.poolId = poolId;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getProgressBlockid() {
        return progressBlockid;
    }

    public void setProgressBlockid(int progressBlockid) {
        this.progressBlockid = progressBlockid;
    }

    public int getRwIops() {
        return rwIops;
    }

    public void setRwIops(int rwIops) {
        this.rwIops = rwIops;
    }

    public int getRwMbytes() {
        return rwMbytes;
    }

    public void setRwMbytes(int rwMbytes) {
        this.rwMbytes = rwMbytes;
    }

    public String getSnapSourceVol() {
        return snapSourceVol;
    }

    public void setSnapSourceVol(String snapSourceVol) {
        this.snapSourceVol = snapSourceVol;
    }

    public int getSnapid() {
        return snapid;
    }

    public void setSnapid(int snapid) {
        this.snapid = snapid;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public int getTaskType() {
        return taskType;
    }

    public void setTaskType(int taskType) {
        this.taskType = taskType;
    }

    public int getTreeid() {
        return treeid;
    }

    public void setTreeid(int treeid) {
        this.treeid = treeid;
    }

    public long getUpdateAt() {
        return updateAt;
    }

    public void setUpdateAt(long updateAt) {
        this.updateAt = updateAt;
    }

    public int getUssId() {
        return ussId;
    }

    public void setUssId(int ussId) {
        this.ussId = ussId;
    }
}
