package org.zstack.header.vipQos;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.network.service.vip.VipVO;

/**
 * Created by liangbo.zhou on 17-6-10.
 */
@RestRequest(
        path = "/vips/{uuid}/vip-qos",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteVipQosEvent.class
)
public class APIDeleteVipQosMsg extends APIMessage {
    @APIParam(resourceType = VipVO.class)
    private String uuid;

    @APIParam(required = false)
    private Integer port;

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public static APIDeleteVipQosMsg __example__() {
        APIDeleteVipQosMsg msg = new APIDeleteVipQosMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
