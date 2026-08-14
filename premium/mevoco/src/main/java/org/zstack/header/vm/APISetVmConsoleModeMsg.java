package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;

@RestRequest(
        path = "/vm-instances/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APISetVmConsoleModeEvent.class
)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "VmUuidDirectResolver", field = "uuid")
public class APISetVmConsoleModeMsg extends APIMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String uuid;
    @APIParam
    private String mode;

    @Override
    public String getVmInstanceUuid() {
        return uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public static APISetVmConsoleModeMsg __example__() {
        APISetVmConsoleModeMsg msg = new APISetVmConsoleModeMsg();
        msg.uuid = uuid();
        msg.mode = "vnc";
        return msg;
    }
}
