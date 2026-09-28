package org.zstack.pciDevice;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.pciDevice.header.HostIommuStateType;

@RestResponse(fieldsTo = {"all"})
public class APIGetHostIommuStateReply extends APIReply {
    private HostIommuStateType state;

    public HostIommuStateType getState() {
        return state;
    }

    public void setState(HostIommuStateType state) {
        this.state = state;
    }

    public static APIGetHostIommuStateReply __example__() {
        APIGetHostIommuStateReply reply = new APIGetHostIommuStateReply();
        reply.setState(HostIommuStateType.Enabled);

        return reply;
    }
}
