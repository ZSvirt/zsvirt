package org.zstack.header.bonding;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO;

@RestRequest(
        path = "/hosts/bondings/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteBondingEvent.class
)
public class APIDeleteBondingMsg extends APIDeleteMessage implements BondingMessage {
    /**
     * @desc uuid of bonding which is going to delete
     */
    @APIParam(successIfResourceNotExisting = true, resourceType = HostNetworkBondingVO.class)
    private String uuid;

    @Override
    public String getBondingUuid() {
        return uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIDeleteBondingMsg __example__() {
        APIDeleteBondingMsg msg = new APIDeleteBondingMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
