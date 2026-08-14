package org.zstack.externalservice.vops;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.utils.gson.JSONObjectUtil;
import com.google.gson.JsonElement;

public enum VOpsErrors {
    GENERAL_ERROR(1000),
    HTTP_TIMED_OUT(1312),
    REMOTE_AGENT_ERROR(1315),
    ;

    private String code;

    private VOpsErrors(int id) {
        code = String.format("VOPS.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }

    public static ErrorCode wrapErrorFromVOpsClient(JsonElement json) {
        try {
            return JSONObjectUtil.rehashObject(json, ErrorCode.class);
        } catch (Exception e) {
            return new ErrorCode(VOpsErrors.GENERAL_ERROR.toString(), json.toString());
        }
    }
}
