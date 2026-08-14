package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by mingjian.deng on 17/1/11.
 */
@RestRequest(
        path = "/vm-instances/{uuid}/nic-qos",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteNicQosEvent.class
)
public class APIDeleteNicQosMsg extends APIMessage {
    @APIParam(resourceType = VmNicVO.class)
    private String uuid;

    @APIParam(validValues = {"in", "out"})
    private String direction;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }
 
    public static APIDeleteNicQosMsg __example__() {
        APIDeleteNicQosMsg msg = new APIDeleteNicQosMsg();
        msg.setUuid(uuid());
        msg.setDirection("in");
        return msg;
    }

}
