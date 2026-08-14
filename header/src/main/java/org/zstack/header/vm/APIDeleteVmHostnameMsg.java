package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;

/**
 * Created by frank on 2/26/2016.
 */
@RestRequest(
        path = "/vm-instances/{uuid}/hostnames",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteVmHostnameEvent.class
)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "VmUuidDirectResolver", field = "uuid")
public class APIDeleteVmHostnameMsg extends APIDeleteMessage implements VmInstanceMessage {
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
 
    public static APIDeleteVmHostnameMsg __example__() {
        APIDeleteVmHostnameMsg msg = new APIDeleteVmHostnameMsg();
        msg.uuid = uuid();
        return msg;
    }
}
