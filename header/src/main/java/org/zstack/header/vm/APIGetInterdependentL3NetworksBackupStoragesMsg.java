package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.zone.ZoneVO;

import java.util.List;

/**
 * Created by Qi Le on 2022/3/9
 */
@RestRequest(
        path = "/backupStorage-l3networks/dependencies",
        method = HttpMethod.GET,
        responseClass = APIGetInterdependentL3NetworksBackupStoragesReply.class
)
public class APIGetInterdependentL3NetworksBackupStoragesMsg extends APISyncCallMessage {
    @APIParam(resourceType = ZoneVO.class)
    private String zoneUuid;
    @APIParam(resourceType = BackupStorageVO.class, required = false)
    private String backupStorageUuid;
    @APIParam(resourceType = L3NetworkVO.class, required = false, nonempty = true)
    private List<String> l3NetworkUuids;

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public List<String> getL3NetworkUuids() {
        return l3NetworkUuids;
    }

    public void setL3NetworkUuids(List<String> l3NetworkUuids) {
        this.l3NetworkUuids = l3NetworkUuids;
    }

    public static APIGetInterdependentL3NetworksBackupStoragesMsg __example__() {
        APIGetInterdependentL3NetworksBackupStoragesMsg msg = new APIGetInterdependentL3NetworksBackupStoragesMsg();
        msg.setZoneUuid(uuid());
        msg.setBackupStorageUuid(uuid());
        return msg;
    }
}
