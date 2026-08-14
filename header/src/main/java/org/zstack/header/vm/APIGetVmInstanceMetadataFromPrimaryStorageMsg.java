package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/primary-storage/vm-instances/metadata",
        method = HttpMethod.GET,
        responseClass = APIGetVmInstanceMetadataFromPrimaryStorageEvent.class
)
public class APIGetVmInstanceMetadataFromPrimaryStorageMsg extends APIMessage implements VmInstanceMessage {
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
        return uuid;
    }

    public static APIGetVmInstanceMetadataFromPrimaryStorageMsg __example__() {
        APIGetVmInstanceMetadataFromPrimaryStorageMsg msg = new APIGetVmInstanceMetadataFromPrimaryStorageMsg();
        msg.setUuid(uuid(VmInstanceVO.class));
        return msg;
    }
}
