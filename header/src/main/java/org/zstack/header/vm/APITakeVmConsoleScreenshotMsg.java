package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * @author shanshan.ning
 * @date 2023-09-11
 */
@RestRequest(
        path = "/vm-instances/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APITakeVmConsoleScreenshotEvent.class
)
public class APITakeVmConsoleScreenshotMsg extends APIMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return getUuid();
    }

    public static APITakeVmConsoleScreenshotMsg __example__() {
        APITakeVmConsoleScreenshotMsg msg = new APITakeVmConsoleScreenshotMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
