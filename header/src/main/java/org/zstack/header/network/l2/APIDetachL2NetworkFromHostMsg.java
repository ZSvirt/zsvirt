package org.zstack.header.network.l2;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/l2-networks/{l2NetworkUuid}/hosts/{hostUuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDetachL2NetworkFromHostEvent.class
)
public class APIDetachL2NetworkFromHostMsg extends APIMessage implements L2NetworkMessage {
    /**
     * @desc l2Network uuid
     */
    @APIParam(resourceType = L2NetworkVO.class)
    private String l2NetworkUuid;
    /**
     * @desc host uuid. See :ref:`HostInventory`
     */
    @APIParam(resourceType = HostVO.class)
    private String hostUuid;

    @Override
    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public static APIDetachL2NetworkFromHostMsg __example__() {
        APIDetachL2NetworkFromHostMsg msg = new APIDetachL2NetworkFromHostMsg();
        msg.setL2NetworkUuid(uuid());
        msg.setHostUuid(uuid());

        return msg;
    }
}
