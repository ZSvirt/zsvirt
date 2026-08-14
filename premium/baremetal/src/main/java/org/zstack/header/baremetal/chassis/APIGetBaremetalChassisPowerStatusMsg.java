package org.zstack.header.baremetal.chassis;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by GuoYi on 7/16/18.
 */
@RestRequest(
        path = "/baremetal/chassis/{uuid}/powerstatus",
        method = HttpMethod.GET,
        responseClass = APIGetBaremetalChassisPowerStatusReply.class
)
public class APIGetBaremetalChassisPowerStatusMsg extends APISyncCallMessage {
    @APIParam(resourceType = BaremetalChassisVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIGetBaremetalChassisPowerStatusMsg __example__() {
        APIGetBaremetalChassisPowerStatusMsg msg = new APIGetBaremetalChassisPowerStatusMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
