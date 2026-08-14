package org.zstack.header.image;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/images/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APISetImageBootModeEvent.class
)
public class APISetImageBootModeMsg extends APIMessage implements ImageMessage {
    @APIParam(resourceType = ImageVO.class)
    private String uuid;

    @APIParam(validValues = {"Legacy", "UEFI", "UEFI_WITH_CSM"})
    private String bootMode;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getBootMode() {
        return bootMode;
    }

    public void setBootMode(String bootMode) {
        this.bootMode = bootMode;
    }

    public static APISetImageBootModeMsg __example__() {
        APISetImageBootModeMsg msg = new APISetImageBootModeMsg();
        msg.setUuid(uuid());
        msg.setBootMode("Legacy");
        return msg;
    }

    @Override
    public String getImageUuid() {
        return uuid;
    }
}
