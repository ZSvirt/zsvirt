package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/hosts/network-facts/{hostUuid}",
        method = HttpMethod.GET,
        responseClass = APIGetHostNetworkFactsReply.class
)
public class APIGetHostNetworkFactsMsg extends APISyncCallMessage {
    @APIParam(resourceType = HostVO.class)
    private String hostUuid;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public static APIGetHostNetworkFactsMsg __example__() {
        APIGetHostNetworkFactsMsg msg = new APIGetHostNetworkFactsMsg();
        msg.setHostUuid(uuid());
        return msg;
    }
}
