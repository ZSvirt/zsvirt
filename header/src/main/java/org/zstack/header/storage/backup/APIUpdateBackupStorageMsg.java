package org.zstack.header.storage.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 6/14/2015.
 */
@RestRequest(
        path = "/backup-storage/{uuid}/actions",
        responseClass = APIUpdateBackupStorageEvent.class,
        method = HttpMethod.PUT,
        isAction = true
)
public class APIUpdateBackupStorageMsg extends APIMessage implements BackupStorageMessage {
    @APIParam(resourceType = BackupStorageVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    @Override
    public String getBackupStorageUuid() {
        return uuid;
    }
 
    public static APIUpdateBackupStorageMsg __example__() {
        APIUpdateBackupStorageMsg msg = new APIUpdateBackupStorageMsg();

        msg.setUuid(uuid());
        msg.setName("New Name");

        return msg;
    }
}
