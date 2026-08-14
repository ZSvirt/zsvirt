package org.zstack.header.vm;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import java.util.ArrayList;
import java.util.List;

@RestResponse(allTo = "networkServices")
public class APIGetVmNicAttachedNetworkServiceReply extends APIReply {
    List<String> networkServices;

    public List<String> getNetworkServices() {
        return networkServices;
    }

    public void setNetworkServices(List<String> networkServices) {
        this.networkServices = networkServices;
    }

    public static APIGetVmNicAttachedNetworkServiceReply __example__() {
        APIGetVmNicAttachedNetworkServiceReply reply = new APIGetVmNicAttachedNetworkServiceReply();
        ArrayList<String> networkServices = new ArrayList<>();
        networkServices.add("Eip");
        reply.setNetworkServices(networkServices);
        return reply;
    }
}
