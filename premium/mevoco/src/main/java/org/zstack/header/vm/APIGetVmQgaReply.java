package org.zstack.header.vm;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * Created by mingjian.deng on 17/1/4.
 */
@RestResponse(fieldsTo = {"enable"})
public class APIGetVmQgaReply extends APIReply {
    boolean enable = false;

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }
 
    public static APIGetVmQgaReply __example__() {
        APIGetVmQgaReply reply = new APIGetVmQgaReply();
        reply.setEnable(true);
        return reply;
    }

}
