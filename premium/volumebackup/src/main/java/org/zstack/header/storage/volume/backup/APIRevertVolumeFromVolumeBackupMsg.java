package org.zstack.header.storage.volume.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.VolumeBackupVO;
import org.zstack.header.volume.VolumeVO;

import java.util.concurrent.TimeUnit;

@RestRequest(
        path = "/volume-backups/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIRevertVolumeFromVolumeBackupEvent.class
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 72)
public class APIRevertVolumeFromVolumeBackupMsg extends APIMessage implements APIAuditor {
    @APIParam(resourceType = VolumeBackupVO.class)
    private String uuid;

    @APIParam(resourceType = BackupStorageVO.class, required = false)
    private String backupStorageUuid;

    @APINoSee
    private String volumeUuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getBackupStrogeUuid() {
        return backupStorageUuid;
    }

    public void setBackupStrogeUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public static APIRevertVolumeFromVolumeBackupMsg __example__() {
        APIRevertVolumeFromVolumeBackupMsg msg = new APIRevertVolumeFromVolumeBackupMsg();

        msg.setUuid(uuid());
        msg.setBackupStrogeUuid(uuid());

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        String volumeUuid = ((APIRevertVolumeFromVolumeBackupMsg) msg).volumeUuid;
        return volumeUuid == null ? null : new Result(volumeUuid, VolumeVO.class);
    }
}
