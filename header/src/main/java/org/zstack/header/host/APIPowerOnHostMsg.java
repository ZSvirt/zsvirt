package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * @Author : jingwang
 * @create 2023/4/14 5:27 PM
 */
@RestRequest(
        path = "/hosts/power/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIPowerOnHostEvent.class,
        isAction = true
)
public class APIPowerOnHostMsg extends APIMessage implements HostMessage {
    @APIParam(nonempty = true, resourceType = HostVO.class)
    private String uuid;
    @APIParam(required = false)
    private boolean returnEarly = false;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public boolean isReturnEarly() {
        return returnEarly;
    }

    public void setReturnEarly(boolean returnEarly) {
        this.returnEarly = returnEarly;
    }

    @Override
    public String getHostUuid() {
        return uuid;
    }

    public static APIPowerOnHostMsg __example__() {
        APIPowerOnHostMsg msg = new APIPowerOnHostMsg();
        msg.setUuid(uuid());
        msg.setReturnEarly(false);
        return msg;
    }
}
