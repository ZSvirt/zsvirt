package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;


/**
 * @Author : jingwang
 * @create 2023/4/14 5:24 PM
 */
@RestRequest(
        path = "/hosts/power/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIShutdownHostEvent.class,
        isAction = true
)
public class APIShutdownHostMsg extends APIMessage implements HostMessage {
    @APIParam(nonempty = true, resourceType = HostVO.class)
    private String uuid;
    @APIParam(required = false)
    private boolean returnEarly = false;
    @APIParam(required = false)
    private boolean force = false;
    @APIParam(required = false, validValues = {"AUTO","AGENT","IPMI"})
    private String method = HostPowerManagementMethod.AUTO.toString();

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
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

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public static APIShutdownHostMsg __example__() {
        APIShutdownHostMsg msg = new APIShutdownHostMsg();
        msg.setUuid(uuid());
        msg.setForce(false);
        msg.setMethod(HostPowerManagementMethod.AUTO.name());
        msg.setReturnEarly(false);
        return msg;
    }
}
