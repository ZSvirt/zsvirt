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
        path = "/vips/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APISetVipQosEvent.class
)
public class APISetVipQosMsg extends APIMessage {

    @APIParam(resourceType = VipVO.class)
    private String uuid;
    @APIParam(required = false, numberRange={1, 65535})
    private Integer port;
    @APIParam(required = false, numberRange={131072L, 32212254720L})
    private Long outboundBandwidth;
    @APIParam(required = false, numberRange={131072L, 32212254720L})
    private Long inboundBandwidth;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setOutboundBandwidth(Long outboundBandwidth) {
        this.outboundBandwidth = outboundBandwidth;
    }

    public void setInboundBandwidth(Long inboundBandwidth) {
        this.inboundBandwidth = inboundBandwidth;
    }

    public Long getOutboundBandwidth() {
        return outboundBandwidth;
    }

    public Long getInboundBandwidth() {
        return inboundBandwidth;
    }

    public String getVipUuid() {
        return uuid;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public static APISetVipQosMsg __example__() {
        APISetVipQosMsg msg = new APISetVipQosMsg();
        msg.setUuid(uuid());
        msg.setPort(22);
        msg.setInboundBandwidth(new Long(1024 * 1024L));
        msg.setOutboundBandwidth(new Long(1024 * 1024L));
        return msg;
    }
}
