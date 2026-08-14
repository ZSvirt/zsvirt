package org.zstack.header.network.l3;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

@Action(category = L3NetworkConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/l3-networks/reserved-ip-ranges/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteReservedIpRangeEvent.class
)
public class APIDeleteReservedIpRangeMsg extends APIDeleteMessage implements L3NetworkMessage, IpRangeMessage {
    /**
     * @desc ip range uuid
     */
    @APIParam(resourceType = ReservedIpRangeVO.class, successIfResourceNotExisting = true,
            checkAccount = true, operationTarget = true)
    private String uuid;
    /**
     * @ignore
     */
    @APINoSee
    private String l3NetworkUuid;

    public APIDeleteReservedIpRangeMsg() {
    }

    public APIDeleteReservedIpRangeMsg(String uuid) {
        super();
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    @Override
    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    @Override
    public String getIpRangeUuid() {
        return uuid;
    }
 
    public static APIDeleteReservedIpRangeMsg __example__() {
        APIDeleteReservedIpRangeMsg msg = new APIDeleteReservedIpRangeMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
