package org.zstack.header.storage.snapshot;

import org.zstack.header.message.NeedReplyMessage;

/**
 */
public class VolumeSnapshotPrimaryStorageDeletionMsg extends NeedReplyMessage implements VolumeSnapshotMessage {
    private String uuid;
    private boolean deleteOnPrimaryStorage;
    private String volumeUuid;
    private boolean volumeDelete = false;
    /**
     * @ignore
     */
    private String treeUuid;

    @Override
    public String getTreeUuid() {
        return treeUuid;
    }

    @Override
    public void setTreeUuid(String treeUuid) {
        this.treeUuid = treeUuid;
    }

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public boolean isDeleteOnPrimaryStorage() {
        return deleteOnPrimaryStorage;
    }

    public void setDeleteOnPrimaryStorage(boolean deleteOnPrimaryStorage) {
        this.deleteOnPrimaryStorage = deleteOnPrimaryStorage;
    }

    @Override
    public String getSnapshotUuid() {
        return getUuid();
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public boolean isVolumeDelete() {
        return volumeDelete;
    }

    public void setVolumeDelete(boolean volumeDelete) {
        this.volumeDelete = volumeDelete;
    }
}
