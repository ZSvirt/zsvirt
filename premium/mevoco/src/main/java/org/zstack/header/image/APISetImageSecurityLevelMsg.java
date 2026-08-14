package org.zstack.header.image;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/images/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APISetImageSecurityLevelEvent.class
)
public class APISetImageSecurityLevelMsg extends APIMessage implements ImageMessage {
    @APIParam(resourceType = ImageVO.class)
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

    public static APISetImageSecurityLevelMsg __example__() {
        APISetImageSecurityLevelMsg msg = new APISetImageSecurityLevelMsg();
        msg.setUuid(uuid());
        msg.setSecurityLevel("low");

        return msg;
    }

    @Override
    public String getImageUuid() {
        return uuid;
    }
}
