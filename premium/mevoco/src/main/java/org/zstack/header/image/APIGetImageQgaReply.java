package org.zstack.header.image;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * Created by mingjian.deng on 17/1/4.
 */
@RestResponse(fieldsTo = {"enable"})
public class APIGetImageQgaReply extends APIReply {
    boolean enable = false;

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }
 
    public static APIGetImageQgaReply __example__() {
        APIGetImageQgaReply reply = new APIGetImageQgaReply();

        return reply;
    }

}
