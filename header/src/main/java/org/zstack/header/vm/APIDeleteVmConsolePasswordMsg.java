package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;

/**
 * Created by root on 8/2/16.
 */
@RestRequest(
        path = "/vm-instances/{uuid}/console-password",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteVmConsolePasswordEvent.class
)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "VmUuidDirectResolver", field = "uuid")
public class APIDeleteVmConsolePasswordMsg extends APIMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String uuid;

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

    public static APIDeleteVmConsolePasswordMsg __example__() {
        APIDeleteVmConsolePasswordMsg msg = new APIDeleteVmConsolePasswordMsg();
        msg.uuid = uuid();
        return msg;
    }
}

