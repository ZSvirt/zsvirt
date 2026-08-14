package org.zstack.header.storage.volume.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageMessage;
import org.zstack.header.storage.backup.BackupStorageVO;

@RestRequest(
        path = "/vm-backups/{groupUuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIRecoverVmBackupFromImageStoreBackupStorageEvent.class
)
public class APIRecoverVmBackupFromImageStoreBackupStorageMsg extends APIMessage implements BackupStorageMessage {
    @APIParam
    private String groupUuid;

    @APIParam(resourceType = BackupStorageVO.class)
    private String srcBackupStorageUuid;

    @APIParam(resourceType = BackupStorageVO.class)
    private String dstBackupStorageUuid;

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }

    public String getSrcBackupStorageUuid() {
        return srcBackupStorageUuid;
    }

    public void setSrcBackupStorageUuid(String srcBackupStorageUuid) {
        this.srcBackupStorageUuid = srcBackupStorageUuid;
    }

    public String getDstBackupStorageUuid() {
        return dstBackupStorageUuid;
    }

    public void setDstBackupStorageUuid(String dstBackupStorageUuid) {
        this.dstBackupStorageUuid = dstBackupStorageUuid;
    }

    public static APIRecoverVmBackupFromImageStoreBackupStorageMsg __example__() {
        APIRecoverVmBackupFromImageStoreBackupStorageMsg msg = new APIRecoverVmBackupFromImageStoreBackupStorageMsg();

        msg.setGroupUuid(uuid());
        msg.setSrcBackupStorageUuid(uuid());
        msg.setDstBackupStorageUuid(uuid());

        return msg;
    }

    @Override
    public String getBackupStorageUuid() {
        return srcBackupStorageUuid;
    }
}
