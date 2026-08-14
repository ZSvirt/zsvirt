package org.zstack.header.storage.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.zone.ZoneVO;

/**
 * @api attach backup storage to a zone
 * @cli
 * @httpMsg {
 * "org.zstack.header.storage.backup.APIAttachBackupStorageMsg": {
 * "zoneUuid": "587a9a14108b48e58c6c1030dbabc657",
 * "backupStorageUuid": "a6f2bb191b63442297d26f8abe15eb58",
 * "session": {
 * "uuid": "4b68addf4b2548b7811a4bf73de71b9e"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.storage.backup.APIAttachBackupStorageMsg": {
 * "zoneUuid": "587a9a14108b48e58c6c1030dbabc657",
 * "backupStorageUuid": "a6f2bb191b63442297d26f8abe15eb58",
 * "session": {
 * "uuid": "4b68addf4b2548b7811a4bf73de71b9e"
 * },
 * "timeout": 1800000,
 * "id": "4f80348320c74be7bed0aede5e1a0a81",
 * "serviceId": "api.portal"
 * }
 * }
 * @result See :ref:`APIAttachBackupStorageEvent`
 * @since 0.1.0
 */
@RestRequest(
        path = "/zones/{zoneUuid}/backup-storage/{backupStorageUuid}",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIAttachBackupStorageToZoneEvent.class
)
public class APIAttachBackupStorageToZoneMsg extends APIMessage implements BackupStorageMessage {
    /**
     * @desc zone uuid. See :ref:`ZoneInventory`
     */
    @APIParam(resourceType = ZoneVO.class)
    private String zoneUuid;
    /**
     * @desc backup storage uuid
     */
    @APIParam(resourceType = BackupStorageVO.class)
    private String backupStorageUuid;

    public APIAttachBackupStorageToZoneMsg() {
    }

    public APIAttachBackupStorageToZoneMsg(String zoneUuid, String uuid) {
        super();
        this.zoneUuid = zoneUuid;
        this.backupStorageUuid = uuid;
    }


    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }


    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }


    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }
 
    public static APIAttachBackupStorageToZoneMsg __example__() {
        APIAttachBackupStorageToZoneMsg msg = new APIAttachBackupStorageToZoneMsg();

        msg.setBackupStorageUuid(uuid());
        msg.setZoneUuid(uuid());

        return msg;
    }
}
