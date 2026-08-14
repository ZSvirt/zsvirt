package org.zstack.header.storage.snapshot;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.storage.backup.BackupStorageVO;

/**
 * @api back up volume snapshot to backup storage
 * @category volume snapshot
 * @cli
 * @httpMsg {
 * "org.zstack.header.storage.snapshot.APIBackupVolumeSnapshotMsg": {
 * "uuid": "b86f375d5ebf455b8037021f8e641fc8",
 * "session": {
 * "uuid": "cb3ffca02e214c10a0ed8b8bb54cdf97"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.storage.snapshot.APIBackupVolumeSnapshotMsg": {
 * "uuid": "b86f375d5ebf455b8037021f8e641fc8",
 * "session": {
 * "uuid": "cb3ffca02e214c10a0ed8b8bb54cdf97"
 * },
 * "timeout": 1800000,
 * "id": "1f3a2ab2c26a4b309992b231118723a6",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APIBackupVolumeSnapshotEvent`
 * @since 0.1.0
 */

public class APIBackupVolumeSnapshotMsg extends APIMessage implements VolumeSnapshotMessage {
    /**
     * @desc volume snapshot uuid
     */
    @APIParam(resourceType = VolumeSnapshotVO.class)
    private String uuid;
    /**
     * @desc uuid of backup storage where the snapshot is being backed up. If omitted, zstack
     * will try to find a proper one
     * @nullable
     */
    @APIParam(required = false, resourceType = BackupStorageVO.class)
    private String backupStorageUuid;

    /**
     * @ignore
     */
    @APINoSee
    private String volumeUuid;

    /**
     * @ignore
     */
    @APINoSee
    private String treeUuid;

    @Override
    public String getTreeUuid() {
        return treeUuid;
    }

    @Override
    public void setTreeUuid(String treeUuid) {
        this.treeUuid = treeUuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    @Override
    public String getSnapshotUuid() {
        return getUuid();
    }

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }


    public static APIBackupVolumeSnapshotMsg __example__() {
        APIBackupVolumeSnapshotMsg msg = new APIBackupVolumeSnapshotMsg();
        return msg;
    }
}