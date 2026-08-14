package org.zstack.header.vm;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * Created by xing5 on 2016/4/8.
 */

@RestResponse(allTo = "hostname")
public class APIGetVmHostnameReply extends APIReply {
    private String hostname;

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
 
    public static APIGetVmHostnameReply __example__() {
        APIGetVmHostnameReply reply = new APIGetVmHostnameReply();
        reply.hostname = "vm1.zstack.orgl";
        return reply;
    }

}
