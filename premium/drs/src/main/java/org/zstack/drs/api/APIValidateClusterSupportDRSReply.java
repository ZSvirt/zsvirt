package org.zstack.drs.api;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * Created by lining on 2019/12/12.
 */
@RestResponse(fieldsTo = "all")
public class APIValidateClusterSupportDRSReply extends APIReply {
    private boolean supported;

    private ErrorCode reason;

    public boolean isSupported() {
        return supported;
    }

    public void setSupported(boolean supported) {
        this.supported = supported;
    }

    public ErrorCode getReason() {
        return reason;
    }

    public void setReason(ErrorCode reason) {
        this.reason = reason;
    }
}
