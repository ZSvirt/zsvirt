package org.zstack.pciDevice;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"all"})
public class APIGetHostIommuStatusReply extends APIReply {
    private HostIommuStatusType status;

    public HostIommuStatusType getStatus() {
        return status;
    }

    public void setStatus(HostIommuStatusType status) {
        this.status = status;
    }

    public static APIGetHostIommuStatusReply __example__() {
        APIGetHostIommuStatusReply reply = new APIGetHostIommuStatusReply();
        reply.setStatus(HostIommuStatusType.Active);

        return reply;
    }
}
