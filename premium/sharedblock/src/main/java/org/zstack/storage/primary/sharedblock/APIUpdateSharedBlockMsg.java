package org.zstack.storage.primary.sharedblock;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.PrimaryStorageMessage;
import org.zstack.header.storage.primary.PrimaryStorageVO;

@RestRequest(
        path = "/primary-storage/sharedblockgroup/{sharedBlockGroupUuid}/sharedblocks/{uuid}/actions",
        responseClass = APIUpdateSharedBlockEvent.class,
        isAction = true,
        method = HttpMethod.PUT
)
public class APIUpdateSharedBlockMsg extends APIMessage implements PrimaryStorageMessage {
    @APIParam(resourceType = SharedBlockVO.class)
    private String uuid;

    @APIParam(resourceType = SharedBlockGroupVO.class)
    private String sharedBlockGroupUuid;

    @APIParam(maxLength = 255, required = false)
    private String name;

    @APIParam(maxLength = 2048, required = false)
    private String description;

    @APIParam(maxLength = 255, required = false)
    private String diskUuid;

    public APIUpdateSharedBlockMsg() {
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public String getSharedBlockGroupUuid() {
        return sharedBlockGroupUuid;
    }

    public void setSharedBlockGroupUuid(String sharedBlockGroupUuid) {
        this.sharedBlockGroupUuid = sharedBlockGroupUuid;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return sharedBlockGroupUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDiskUuid() {
        return diskUuid;
    }

    public void setDiskUuid(String diskUuid) {
        this.diskUuid = diskUuid;
    }

    public static APIUpdateSharedBlockMsg __example__() {
        APIUpdateSharedBlockMsg msg = new APIUpdateSharedBlockMsg();
        msg.setSharedBlockGroupUuid(uuid());
        msg.setUuid(uuid());
        msg.setName("disk-01");
        msg.setDescription("example");
        msg.setDiskUuid(uuid());
        return msg;
    }
}
