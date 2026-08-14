package org.zstack.header.storage.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/primary-storage/vm-instances/metadata/scan",
        method = HttpMethod.GET,
        responseClass = APIScanVmInstanceMetadataFromPrimaryStorageReply.class
)
public class APIScanVmInstanceMetadataFromPrimaryStorageMsg extends APISyncCallMessage implements PrimaryStorageMessage {
    @APIParam(resourceType = PrimaryStorageVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return uuid;
    }

    public static APIScanVmInstanceMetadataFromPrimaryStorageMsg __example__() {
        APIScanVmInstanceMetadataFromPrimaryStorageMsg msg = new APIScanVmInstanceMetadataFromPrimaryStorageMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
