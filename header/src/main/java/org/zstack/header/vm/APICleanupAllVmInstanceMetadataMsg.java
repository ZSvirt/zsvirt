package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.PrimaryStorageVO;

import java.util.List;

@RestRequest(
        path = "/vm-instances/metadata",
        method = HttpMethod.DELETE,
        responseClass = APICleanupAllVmInstanceMetadataEvent.class
)
public class APICleanupAllVmInstanceMetadataMsg extends APIMessage {
    @APIParam(resourceType = PrimaryStorageVO.class, required = false)
    private List<String> primaryStorageUuids;

    public List<String> getPrimaryStorageUuids() {
        return primaryStorageUuids;
    }

    public void setPrimaryStorageUuids(List<String> primaryStorageUuids) {
        this.primaryStorageUuids = primaryStorageUuids;
    }

    public static APICleanupAllVmInstanceMetadataMsg __example__() {
        APICleanupAllVmInstanceMetadataMsg msg = new APICleanupAllVmInstanceMetadataMsg();
        msg.primaryStorageUuids = java.util.Arrays.asList(uuid(), uuid());
        return msg;
    }
}
