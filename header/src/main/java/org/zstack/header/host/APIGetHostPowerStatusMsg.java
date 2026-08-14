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
        responseClass = APIGetHostPowerStatusEvent.class,
        isAction = true
)
public class APIGetHostPowerStatusMsg extends APIMessage implements HostMessage {
    @APIParam(nonempty = true, resourceType = HostVO.class)
    private String uuid;
    @APIParam(required = false, validValues = {"AUTO","AGENT","IPMI"})
    private String method = HostPowerManagementMethod.AUTO.toString();

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    @Override
    public String getHostUuid() {
        return uuid;
    }

    public static APIGetHostPowerStatusMsg __example__() {
        APIGetHostPowerStatusMsg msg = new APIGetHostPowerStatusMsg();
        msg.setUuid(uuid());
        msg.setMethod(HostPowerManagementMethod.AUTO.name());
        return msg;
    }
}
