package org.zstack.header.baremetal.chassis;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 7/16/18.
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetBaremetalChassisPowerStatusReply extends APIReply {
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public static APIGetBaremetalChassisPowerStatusReply __example__() {
        APIGetBaremetalChassisPowerStatusReply reply = new APIGetBaremetalChassisPowerStatusReply();
        reply.setStatus("ON");
        return reply;
    }
}
