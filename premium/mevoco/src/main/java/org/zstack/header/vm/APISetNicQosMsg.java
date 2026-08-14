package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by mingjian.deng on 16/12/9.
 */
@RestRequest(
        path = "/vm-instances/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APISetNicQosEvent.class
)
public class APISetNicQosMsg extends APIMessage {
    @APIParam(resourceType = VmNicVO.class)
    private String uuid;

    @APIParam(required = false, numberRange={8192, 32212254720L})
    private Long outboundBandwidth;
    @APIParam(required = false, numberRange={8192, 32212254720L})
    private Long inboundBandwidth;

    public Long getOutboundBandwidth() {
        return outboundBandwidth;
    }

    public void setOutboundBandwidth(Long outboundBandwidth) {
        this.outboundBandwidth = outboundBandwidth;
    }

    public Long getInboundBandwidth() {
        return inboundBandwidth;
    }

    public void setInboundBandwidth(Long inboundBandwidth) {
        this.inboundBandwidth = inboundBandwidth;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
 
    public static APISetNicQosMsg __example__() {
        APISetNicQosMsg msg = new APISetNicQosMsg();
        msg.setUuid(uuid());
        msg.setInboundBandwidth(819200l);
        msg.setOutboundBandwidth(819200l);
        return msg;
    }

}
