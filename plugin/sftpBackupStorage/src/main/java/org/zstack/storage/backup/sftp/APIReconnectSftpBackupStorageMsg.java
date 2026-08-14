package org.zstack.storage.backup.sftp;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageMessage;

@RestRequest(
        path = "/backup-storage/sftp/{uuid}/actions",
        isAction = true,
        responseClass = APIReconnectSftpBackupStorageEvent.class,
        method = HttpMethod.PUT
)
public class APIReconnectSftpBackupStorageMsg extends APIMessage implements BackupStorageMessage {
    @APIParam(resourceType = SftpBackupStorageVO.class)
    private String uuid;

    @Override
    public String getBackupStorageUuid() {
        return uuid;
    }

    public void setUuid(String backupStorageUuid) {
        this.uuid = backupStorageUuid;
    }
 
    public static APIReconnectSftpBackupStorageMsg __example__() {
        APIReconnectSftpBackupStorageMsg msg = new APIReconnectSftpBackupStorageMsg();
        msg.setUuid("76d39c6862b840a3aa4568d83db99022");

        return msg;
    }
}
