package org.zstack.loginControl.api;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 *  * Created by LiangHanYu on 2021/2/4 16:07
 *   */
@RestResponse(fieldsTo = {"all"})
public class APIValidatePasswordReply extends APIReply {
    private boolean available = false;

    public boolean getAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public static APIValidatePasswordReply __example__() {
        APIValidatePasswordReply reply = new APIValidatePasswordReply();
        reply.setAvailable(true);
        return reply;
    }
}
