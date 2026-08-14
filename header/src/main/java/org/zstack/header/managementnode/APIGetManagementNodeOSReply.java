package org.zstack.header.managementnode;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * Created by Jialong on 2021/03/15.
 */

@RestResponse(fieldsTo = {"all"})
public class APIGetManagementNodeOSReply extends APIReply {
    private String name;
    private String version;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public static APIGetManagementNodeOSReply __example__(){
        APIGetManagementNodeOSReply reply = new APIGetManagementNodeOSReply();
        reply.setName("Linux");
        reply.setVersion("5.4.0-33-generic");
        return reply;
    }
}
