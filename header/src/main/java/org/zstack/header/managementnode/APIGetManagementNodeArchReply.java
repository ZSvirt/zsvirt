package org.zstack.header.managementnode;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * Created by Jialong on 2021/03/15.
 */

@RestResponse(fieldsTo = {"all"})
public class APIGetManagementNodeArchReply extends APIReply {
    private String architecture;

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public static APIGetManagementNodeArchReply __example__() {
        APIGetManagementNodeArchReply reply= new APIGetManagementNodeArchReply();
        reply.setArchitecture("x86_64");
        return reply;
    }
}
