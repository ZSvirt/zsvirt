package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * author:kaicai.hu
 * Date:2021/8/6
 */
@RestRequest(
        path = "/hosts/physical-memory-facts/{hostUuid}",
        method = HttpMethod.GET,
        responseClass = APIGetHostPhysicalMemoryFactsReply.class
)
public class APIGetHostPhysicalMemoryFactsMsg extends APISyncCallMessage {
    @APIParam(resourceType = HostVO.class)
    private String hostUuid;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public static APIGetHostPhysicalMemoryFactsMsg __example__() {
        APIGetHostPhysicalMemoryFactsMsg msg = new APIGetHostPhysicalMemoryFactsMsg();
        msg.setHostUuid(uuid());
        return msg;
    }
}
