package org.zstack.header.sriov;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 12/9/19.
 */
@RestResponse(allTo = "vfNicAvailable")
public class APIIsVfNicAvailableInL3NetworkReply extends APIReply {
    private boolean vfNicAvailable;

    public boolean isVfNicAvailable() {
        return vfNicAvailable;
    }

    public void setVfNicAvailable(boolean vfNicAvailable) {
        this.vfNicAvailable = vfNicAvailable;
    }

    public static APIIsVfNicAvailableInL3NetworkReply __example__() {
        APIIsVfNicAvailableInL3NetworkReply reply = new APIIsVfNicAvailableInL3NetworkReply();
        reply.setVfNicAvailable(true);
        return reply;
    }
}
