package org.zstack.header.storage.snapshot;

import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.storage.backup.BackupStorageVO;

import java.util.List;

/**
 * @api delete a copy of volume snapshot from one or more backup storage
 * @category volume snapshot
 * @cli
 * @httpMsg {
 * "org.zstack.header.storage.snapshot.APIDeleteVolumeSnapshotFromBackupStorageMsg": {
 * "uuid": "789f13b8e9b84e44888b113e55c6e776",
 * "backupStorageUuids": [
 * "1da98b69748a4ef39eabc74f064a151a"
 * ],
 * "deleteMode": "Permissive",
 * "session": {
 * "uuid": "f6a11ad55751458d9623174ef7fce8de"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.storage.snapshot.APIDeleteVolumeSnapshotFromBackupStorageMsg": {
 * "uuid": "789f13b8e9b84e44888b113e55c6e776",
 * "backupStorageUuids": [
 * "1da98b69748a4ef39eabc74f064a151a"
 * ],
 * "deleteMode": "Permissive",
 * "session": {
 * "uuid": "f6a11ad55751458d9623174ef7fce8de"
 * },
 * "timeout": 1800000,
 * "id": "b0225e5fc5df4c13ad2ab8b04a42aaff",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APIDeleteVolumeSnapshotFromBackupStorageEvent`
 * @since 0.1.0
 */
public class APIDeleteVolumeSnapshotFromBackupStorageMsg extends APIDeleteMessage implements VolumeSnapshotMessage {
    /**
     * @desc volume snapshot uuid
     */
    @APIParam(resourceType = VolumeSnapshotVO.class)
    private String uuid;
    /**
     * @desc a list of backup storage uuid where snapshot is being deleted from
     */
    @APIParam(resourceType = BackupStorageVO.class)
    private List<String> backupStorageUuids;

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

    public List<String> getBackupStorageUuids() {
        return backupStorageUuids;
    }

    public void setBackupStorageUuids(List<String> backupStorageUuids) {
        this.backupStorageUuids = backupStorageUuids;
    }

    @Override
    public String getSnapshotUuid() {
        return uuid;
    }

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public static APIDeleteVolumeSnapshotFromBackupStorageMsg __example__() {
        APIDeleteVolumeSnapshotFromBackupStorageMsg msg = new APIDeleteVolumeSnapshotFromBackupStorageMsg();
        return msg;
    }
}