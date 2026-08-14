package org.zstack.ha;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * Created by xing5 on 2016/3/29.
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetVmInstanceHaLevelReply extends APIReply {
    private String level;

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }
 
    public static APIGetVmInstanceHaLevelReply __example__() {
        APIGetVmInstanceHaLevelReply reply = new APIGetVmInstanceHaLevelReply();
        reply.setLevel("NeverStop");

        return reply;
    }

}
