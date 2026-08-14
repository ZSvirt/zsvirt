package org.zstack.header.vm;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * Created by longtao.wu@zstack.io on 21/12/01
 */
@RestResponse(fieldsTo = {"enable"})
public class APIGetVmNumaReply extends APIReply {
    private boolean enable = false;

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public static APIGetVmNumaReply __example__() {
        APIGetVmNumaReply reply = new APIGetVmNumaReply();
        reply.setEnable(true);
        return reply;
    }

}
