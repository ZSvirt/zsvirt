package org.zstack.header.storage.addon.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.APIAddBackupStorageMsg;
import org.zstack.header.storage.backup.BackupStorageConstant;

@RestRequest(
        path = "/backup-storage/addon",
        method = HttpMethod.POST,
        responseClass = APIAddExternalBackupStorageEvent.class,
        parameterName = "params"
)
public class APIAddExternalBackupStorageMsg extends APIAddBackupStorageMsg {
    @APIParam(maxLength = 255, emptyString = false)
    private String identity;

    @Override
    public String getType() {
        return BackupStorageConstant.EXTERNAL_BACKUP_STORAGE_TYPE;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public static APIAddExternalBackupStorageMsg __example__() {
        APIAddExternalBackupStorageMsg msg = new APIAddExternalBackupStorageMsg();
        msg.setIdentity("zbd");
        msg.setName("my backup storage");
        msg.setUrl("zbd:pool/my-vol:/etc/foo.conf");

        return msg;
    }

}
