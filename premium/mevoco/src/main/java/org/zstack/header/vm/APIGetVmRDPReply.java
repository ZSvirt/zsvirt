package org.zstack.header.vm;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * Created by mingjian.deng on 17/1/4.
 */
@RestResponse(fieldsTo = {"enable"})
public class APIGetVmRDPReply extends APIReply {
    private boolean enable = false;

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }
 
    public static APIGetVmRDPReply __example__() {
        APIGetVmRDPReply reply = new APIGetVmRDPReply();
        reply.setEnable(true);
        return reply;
    }

}
