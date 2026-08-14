package org.zstack.header.managementnode;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * Created by Qi Le on 2021/3/15
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetPlatformTimeZoneReply extends APIReply {
    private String timezone;
    private String offset;

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getOffset() {
        return offset;
    }

    public void setOffset(String offset) {
        this.offset = offset;
    }

    public static APIGetPlatformTimeZoneReply __example__() {
        APIGetPlatformTimeZoneReply reply = new APIGetPlatformTimeZoneReply();
        reply.setTimezone("Asia/Shanghai");
        reply.setOffset("+8:00");
        return reply;
    }
}
