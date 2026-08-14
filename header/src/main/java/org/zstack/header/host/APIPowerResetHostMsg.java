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
        responseClass = APIPowerResetHostEvent.class,
        isAction = true
)
public class APIPowerResetHostMsg extends APIMessage implements HostMessage {
    @APIParam(nonempty = true, resourceType = HostVO.class)
    private String uuid;

    @APIParam(required = false)
    private boolean returnEarly = false;

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

    public static APIPowerResetHostMsg __example__() {
        APIPowerResetHostMsg msg = new APIPowerResetHostMsg();
        msg.setUuid(uuid());
        msg.setMethod(HostPowerManagementMethod.AUTO.name());
        msg.setReturnEarly(false);
        return msg;
    }
}
