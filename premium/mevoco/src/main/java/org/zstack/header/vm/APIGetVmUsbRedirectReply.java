package org.zstack.header.vm;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * Created by mingjian.deng on 17/1/4.
 */
@RestResponse(fieldsTo = {"enable"})
public class APIGetVmUsbRedirectReply extends APIReply {
    private boolean enable = false;

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }
 
    public static APIGetVmUsbRedirectReply __example__() {
        APIGetVmUsbRedirectReply reply = new APIGetVmUsbRedirectReply();
        reply.setEnable(true);
        return reply;
    }

}
