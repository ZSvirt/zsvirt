package org.zstack.header.storage.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.zone.ZoneVO;

import java.util.Collections;
import java.util.List;

/**
 */
@RestRequest(
        path = "/backup-storage/capacities",
        method = HttpMethod.GET,
        responseClass = APIGetBackupStorageCapacityReply.class
)
public class APIGetBackupStorageCapacityMsg extends APISyncCallMessage {
    @APIParam(required = false, resourceType = ZoneVO.class)
    private List<String> zoneUuids;
    @APIParam(required = false, resourceType = BackupStorageVO.class)
    private List<String> backupStorageUuids;
    private boolean all;

    public boolean isAll() {
        return all;
    }

    public void setAll(boolean all) {
        this.all = all;
    }

    public List<String> getZoneUuids() {
        return zoneUuids;
    }

    public void setZoneUuids(List<String> zoneUuids) {
        this.zoneUuids = zoneUuids;
    }

    public List<String> getBackupStorageUuids() {
        return backupStorageUuids;
    }

    public void setBackupStorageUuids(List<String> backupStorageUuids) {
        this.backupStorageUuids = backupStorageUuids;
    }
 
    public static APIGetBackupStorageCapacityMsg __example__() {
        APIGetBackupStorageCapacityMsg msg = new APIGetBackupStorageCapacityMsg();

        msg.setZoneUuids(Collections.singletonList(uuid()));
        msg.setAll(true);

        return msg;
    }

}
