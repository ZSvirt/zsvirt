package org.zstack.autoscaling.group.activity.action;

import org.zstack.header.errorcode.ErrorCode;

import java.util.List;

/**
 * Created by lining on 2018/9/14.
 */

/**
 * All successful : success = true , result.errorCodes is empty
 * Partially successful : success = true , result.errorCodes not empty
 * All failed : success = false , errorCode not null
 */
public class CreateInstancesResult {
    List<String> instanceUuids;

    List<ErrorCode> errorCodes;

    public List<String> getInstanceUuids() {
        return instanceUuids;
    }

    public void setInstanceUuids(List<String> instanceUuids) {
        this.instanceUuids = instanceUuids;
    }

    public List<ErrorCode> getErrorCodes() {
        return errorCodes;
    }

    public void setErrorCodes(List<ErrorCode> errorCodes) {
        this.errorCodes = errorCodes;
    }

}
