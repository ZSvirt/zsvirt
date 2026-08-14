package org.zstack.header.storage.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by xing5 on 2016/4/9.
 */
@RestRequest(
        path = "/backup-storage/{uuid}/actions",
        isAction = true,
        responseClass = APIReconnectBackupStorageEvent.class,
        method = HttpMethod.PUT
)
public class APIReconnectBackupStorageMsg extends APIMessage implements BackupStorageMessage {
    @APIParam(resourceType = BackupStorageVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getBackupStorageUuid() {
        return getUuid();
    }
 
    public static APIReconnectBackupStorageMsg __example__() {
        APIReconnectBackupStorageMsg msg = new APIReconnectBackupStorageMsg();

        msg.setUuid(uuid());

        return msg;
    }
}
