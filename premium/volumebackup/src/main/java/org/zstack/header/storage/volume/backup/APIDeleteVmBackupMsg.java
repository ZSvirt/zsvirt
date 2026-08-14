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
import org.zstack.header.storage.backup.VolumeMetadata;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestRequest(
        path = "/vm-backups/{groupUuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteVmBackupEvent.class
)
public class APIDeleteVmBackupMsg extends APIDeleteMessage implements APIAuditor, VolumeBackupDeleteMessage {
    @APIParam
    private String groupUuid;

    @APIParam(required = false, nonempty = true, resourceType = BackupStorageVO.class)
    private List<String> backupStorageUuids;

    @APIParam(required = false)
    private boolean handleDependency;

    @APINoSee
    private List<VolumeBackupInventory> inventories = new ArrayList<>();

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }

    public List<String> getBackupStorageUuids() {
        return backupStorageUuids;
    }

    public void setBackupStorageUuids(List<String> backupStorageUuids) {
        this.backupStorageUuids = backupStorageUuids;
    }

    public static APIDeleteVmBackupMsg __example__() {
        APIDeleteVmBackupMsg msg = new APIDeleteVmBackupMsg();

        msg.setGroupUuid(uuid());
        msg.setBackupStorageUuids(Collections.singletonList(uuid()));

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return ((APIDeleteVmBackupMsg) msg).inventories.stream().findFirst()
                .map(it -> new Result(JSONObjectUtil.toObject(it.getMetadata(), VolumeMetadata.class)
                        .getVmInstanceUuid(), VmInstanceVO.class))
                .orElse(null);
    }

    public List<VolumeBackupInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VolumeBackupInventory> inventories) {
        this.inventories = inventories;
    }

    @Override
    public String getVolumeBackupUuid() {
        return inventories.stream().findFirst().map(VolumeBackupInventory::getUuid).orElse(null);
    }

    @Override
    public boolean isHandleDependency() {
        return handleDependency;
    }

    public void setHandleDependency(boolean handleDependency) {
        this.handleDependency = handleDependency;
    }
}
