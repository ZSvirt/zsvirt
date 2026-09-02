package org.zstack.header.errorcode;

import org.apache.commons.collections.CollectionUtils;

/**
 * Created by mingjian.deng on 2020/3/25.
 */
public class JobResultError {
    private String detail;
    private String cause;

    public JobResultError() {}

    public JobResultError(String detail) {
        this.detail = detail;
    }

    public static JobResultError valueOf(ErrorCode error) {
        if (!CollectionUtils.isEmpty(error.getCauses())) {
            return parseErrorCodeWithCauses(error);
        }
        return parseErrorCode(error);
    }

    private static JobResultError parseErrorCode(ErrorCode error) {
        JobResultError result = new JobResultError(error.getDetails());
        result.setCause(error.getRootCauseDetails());
        return result;
    }

    public static JobResultError parseErrorCodeWithCauses(ErrorCode error) {
        JobResultError result = new JobResultError(error.getDetails());
        result.setCause(error.getReadableDetails());
        return result;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getCause() {
        return cause;
    }

    public void setCause(String cause) {
        this.cause = cause;
    }
}
