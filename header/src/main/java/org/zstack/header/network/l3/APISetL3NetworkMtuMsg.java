package org.zstack.header.network.l3;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by weiwang on 18/05/2017.
 */
@RestRequest(
        path = "/l3-networks/{l3NetworkUuid}/mtu",
        method = HttpMethod.POST,
        responseClass = APISetL3NetworkMtuEvent.class,
        parameterName = "params"
)
public class APISetL3NetworkMtuMsg extends APIMessage implements L3NetworkMessage {
    @APIParam(resourceType = L3NetworkVO.class)
    private String l3NetworkUuid;

    @APIParam(numberRange = {68, 9216})
    private Integer mtu;

    @Override
    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public Integer getMtu() {
        return mtu;
    }

    public void setMtu(Integer mtu) {
        this.mtu = mtu;
    }

    public static APISetL3NetworkMtuMsg __example__() {
        APISetL3NetworkMtuMsg msg = new APISetL3NetworkMtuMsg();
        msg.setL3NetworkUuid(uuid());
        msg.setMtu(9216);

        return msg;
    }
}
