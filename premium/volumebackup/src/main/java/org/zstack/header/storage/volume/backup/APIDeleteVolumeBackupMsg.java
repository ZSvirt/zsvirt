package org.zstack.header.storage.volume.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.VolumeBackupInventory;
import org.zstack.header.storage.backup.VolumeBackupVO;
import org.zstack.header.volume.VolumeVO;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RestRequest(
        path = "/volume-backups/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteVolumeBackupEvent.class
)
public class APIDeleteVolumeBackupMsg extends APIDeleteMessage implements VolumeBackupDeleteMessage, APIAuditor {
    /**
     * @desc volume backup uuid
     */
    @APIParam(resourceType = VolumeBackupVO.class)
    private String uuid;

    @APIParam(required = false, nonempty = true, resourceType = BackupStorageVO.class)
    private List<String> backupStorageUuids;

    @APIParam(required = false)
    private boolean handleDependency;

    @APINoSee
    private VolumeBackupInventory inventory;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public List<String> getBackupStorageUuids() {
        return backupStorageUuids;
    }

    public void setBackupStorageUuids(List<String> backupStorageUuids) {
        this.backupStorageUuids = backupStorageUuids;
    }

    public static APIDeleteVolumeBackupMsg __example__() {
        APIDeleteVolumeBackupMsg msg = new APIDeleteVolumeBackupMsg();

        msg.setBackupStorageUuids(Collections.singletonList(uuid()));
        msg.setUuid(uuid());

        return msg;
    }

    @Override
    public String getVolumeBackupUuid() {
        return uuid;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return Optional.ofNullable(((APIDeleteVolumeBackupMsg)msg).inventory)
                .map(it -> new Result(it.getVolumeUuid(), VolumeVO.class))
                .orElse(null);
    }

    public VolumeBackupInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeBackupInventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public boolean isHandleDependency() {
        return handleDependency;
    }

    public void setHandleDependency(boolean handleDependency) {
        this.handleDependency = handleDependency;
    }
}
