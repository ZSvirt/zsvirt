package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/vm-instances/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APISetVmSecurityLevelEvent.class
)
public class APISetVmSecurityLevelMsg extends APIMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String uuid;
    @APIParam(required = false)
    private String securityLevel;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getSecurityLevel() {
        return securityLevel;
    }

    public void setSecurityLevel(String securityLevel) {
        this.securityLevel = securityLevel;
    }

    @Override
    public String getVmInstanceUuid() {
        return getUuid();
    }

    public static APISetVmSecurityLevelMsg __example__() {
        APISetVmSecurityLevelMsg msg = new APISetVmSecurityLevelMsg();
        msg.setUuid(uuid());
        msg.setSecurityLevel("low");

        return msg;
    }
}
