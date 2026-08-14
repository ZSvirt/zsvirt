package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;

/**
 * Created by luchukun on 8/4/16.
 */
@RestRequest(
        path = "/vm-instances/{uuid}/ssh-keys",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteVmSshKeyEvent.class
)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "VmUuidDirectResolver", field = "uuid")
public class APIDeleteVmSshKeyMsg extends APIMessage implements VmInstanceMessage {
    private String uuid;

    @Override
    public String getVmInstanceUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }
 
    public static APIDeleteVmSshKeyMsg __example__() {
        APIDeleteVmSshKeyMsg msg = new APIDeleteVmSshKeyMsg();
        msg.uuid = uuid();
        return msg;
    }
}
