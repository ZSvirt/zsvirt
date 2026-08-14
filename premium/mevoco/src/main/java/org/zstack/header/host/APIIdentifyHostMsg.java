package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Create by weiwang at 2019-04-08
 */
@RestRequest(
        path = "/hosts/kvm/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIIdentifyHostEvent.class,
        isAction = true
)
public class APIIdentifyHostMsg extends APIMessage implements HostMessage {
    @APIParam(resourceType = HostVO.class)
    private String uuid;

    @APIParam(required = false, numberRange = {0, 255})
    private Long interval = 60L;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getHostUuid() {
        return getUuid();
    }

    public Long getInterval() {
        return interval;
    }

    public void setInterval(Long interval) {
        this.interval = interval;
    }

    public static APIIdentifyHostMsg __example__() {
        APIIdentifyHostMsg msg = new APIIdentifyHostMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
