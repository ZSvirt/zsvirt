package org.zstack.header.sriov;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.rest.RestRequest;

/**
 * Created by GuoYi on 12/9/19.
 */
@RestRequest(
        path = "/l3-networks/{l3NetworkUuid}/hosts/{hostUuid}/vfnicavailable",
        responseClass = APIIsVfNicAvailableInL3NetworkReply.class,
        method = HttpMethod.GET
)
public class APIIsVfNicAvailableInL3NetworkMsg extends APISyncCallMessage {
    @APIParam(resourceType = L3NetworkVO.class)
    private String l3NetworkUuid;
    @APIParam(resourceType = HostVO.class)
    private String hostUuid;

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public static APIIsVfNicAvailableInL3NetworkMsg __example__() {
        APIIsVfNicAvailableInL3NetworkMsg msg = new APIIsVfNicAvailableInL3NetworkMsg();
        msg.setL3NetworkUuid(uuid());
        msg.setHostUuid(uuid());
        return msg;
    }
}
