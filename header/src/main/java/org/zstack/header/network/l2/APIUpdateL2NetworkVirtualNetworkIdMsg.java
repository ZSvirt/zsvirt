package org.zstack.header.network.l2;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

/**
 * Created by boce.wang on 03/20/2024.
 */
@RestRequest(
        path = "/l2-networks/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateL2NetworkVirtualNetworkIdEvent.class,
        isAction = true
)
public class APIUpdateL2NetworkVirtualNetworkIdMsg extends APIMessage implements L2NetworkMessage, APIAuditor {
    @APIParam(resourceType = L2NetworkVO.class)
    private String uuid;
    @APIParam
    private Integer virtualNetworkId;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Integer getVirtualNetworkId() {
        return virtualNetworkId;
    }

    public void setVirtualNetworkId(Integer virtualNetworkId) {
        this.virtualNetworkId = virtualNetworkId;
    }

    @Override
    public String getL2NetworkUuid() {
        return uuid;
    }

    public static APIUpdateL2NetworkVirtualNetworkIdMsg __example__() {
        APIUpdateL2NetworkVirtualNetworkIdMsg msg = new APIUpdateL2NetworkVirtualNetworkIdMsg();
        msg.setUuid(uuid());
        msg.setVirtualNetworkId(1);
        return msg;
    }

    @Override
    public APIAuditor.Result audit(APIMessage msg, APIEvent rsp) {
        return new APIAuditor.Result(rsp.isSuccess() ? ((APIUpdateL2NetworkVirtualNetworkIdEvent)rsp).getInventory().getUuid() : "", L2NetworkVO.class);
    }
}
