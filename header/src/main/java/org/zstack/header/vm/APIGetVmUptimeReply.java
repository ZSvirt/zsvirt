package org.zstack.header.vm;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
@RestResponse(fieldsTo = {"uptime"})
public class APIGetVmUptimeReply extends APIReply {
    private String uptime;

    public APIGetVmUptimeReply() {
    }

    public String getUptime() {
        return uptime;
    }

    public void setUptime(String uptime) {
        this.uptime = uptime;
    }

    public static APIGetVmUptimeReply __example__() {
        APIGetVmUptimeReply event = new APIGetVmUptimeReply();
        event.setUptime("2023-10-01 10:12:15");
        return event;
    }

}

