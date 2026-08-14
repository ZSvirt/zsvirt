package org.zstack.autoscaling.group.activity.action;

import org.zstack.header.errorcode.ErrorCode;

import java.util.List;

/**
 * Created by lining on 2018/9/14.
 */
public class RemoveInstancesResult {
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
