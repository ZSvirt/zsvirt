package org.zstack.header.vipQos;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.network.service.vip.VipVO;

/**
 * Created by liangbo.zhou on 17-6-10.
 */
@RestRequest(
        path = "/vip/{uuid}/vip-qos",
        method = HttpMethod.GET,
        responseClass = APIGetVipQosReply.class
)
public class APIGetVipQosMsg extends APISyncCallMessage {
    @APIParam(resourceType = VipVO.class, required = false)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIGetVipQosMsg __example__() {
        APIGetVipQosMsg msg = new APIGetVipQosMsg();
        msg.setUuid(uuid());

        return msg;
    }

}
