package org.zstack.header.managementnode;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 11/14/2015.
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetVersionReply extends APIReply {
    private String version;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
 
    public static APIGetVersionReply __example__() {
        APIGetVersionReply reply = new APIGetVersionReply();
        reply.setVersion("1.9.x");

        return reply;
    }

}
