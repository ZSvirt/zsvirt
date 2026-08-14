package org.zstack.expon.sdk.volume;

import org.zstack.expon.sdk.ExponResponse;

/**
 * @example
 * {
 * 	"message": "",
 * 	"ret_code": "0",
 * 	"task": {
 * 		"cloneid": 0,
 * 		"code": 0,
 * 		"dest_vol": "volume-e6a6c343-0eae-4505-be84-022e85dabf83",
 * 		"first_blockid": 0,
 * 		"last_blockid": 255,
 * 		"msg": "",
 * 		"namespace": "ussns",
 * 		"path": "",
 * 		"pool_id": 1,
 * 		"progress": 100,
 * 		"progress_blockid": 233,
 * 		"rw_iops": 0,
 * 		"rw_mbytes": 0,
 * 		"snap_source_vol": "",
 * 		"snapid": 0,
 * 		"speed": 8,
 * 		"state": "TASK_COMPLETE",
 * 		"task_id": 1,
 * 		"task_type": 3,
 * 		"treeid": 2,
 * 		"update_at": 1704699092,
 * 		"uss_id": 1,
 * 		"vol_name": "snapshot-1f0ba3d1-bff4-473b-af71-d1a74788f8a0",
 * 		"vol_size": 1073741824
 *  }
 * }
 */
public class GetVolumeTaskProgressResponse extends ExponResponse {
    private VolumeTask task;

    public VolumeTask getTask() {
        return task;
    }

    public void setTask(VolumeTask task) {
        this.task = task;
    }
}
