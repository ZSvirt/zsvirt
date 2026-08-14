package org.zstack.header.managementnode;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.List;

@RestResponse(fieldsTo = "all")
public class APIGetSupportAPIsReply extends APIReply {
    private List<String> supportApis;

    public List<String> getSupportApis() {
        return supportApis;
    }

    public void setSupportApis(List<String> supportApis) {
        this.supportApis = supportApis;
    }

    public static APIGetSupportAPIsReply __example__ () {
        APIGetSupportAPIsReply reply = new APIGetSupportAPIsReply();
        reply.setSupportApis(new ArrayList<>());
        return reply;
    }
}
