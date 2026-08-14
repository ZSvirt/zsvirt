package org.zstack.header.vm;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * Created by meilei007@gmail.com on 17/8/19.
 */
@RestResponse(fieldsTo = {"monitorNumber"})
public class APIGetVmMonitorNumberReply extends APIReply {
    private Integer monitorNumber;

    public Integer getMonitorNumber() {
        return monitorNumber;
    }

    public void setMonitorNumber(Integer monitorNumber) {
        this.monitorNumber = monitorNumber;
    }

    public static APIGetVmMonitorNumberReply __example__() {
        APIGetVmMonitorNumberReply reply = new APIGetVmMonitorNumberReply();
        reply.setMonitorNumber(2);
        return reply;
    }

}
