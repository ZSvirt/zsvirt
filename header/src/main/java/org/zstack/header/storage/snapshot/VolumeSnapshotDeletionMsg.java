package org.zstack.header.storage.snapshot;

import org.zstack.header.message.DeletionMessage;

/**
 */
public class VolumeSnapshotDeletionMsg extends DeletionMessage implements VolumeSnapshotMessage {
    private String snapshotUuid;
    private String volumeUuid;
    private boolean volumeDeletion;
    private boolean dbOnly;
    private String direction;
    private String scope;
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

    public boolean isVolumeDeletion() {
        return volumeDeletion;
    }

    public void setVolumeDeletion(boolean volumeDeletion) {
        this.volumeDeletion = volumeDeletion;
    }

    public String getSnapshotUuid() {
        return snapshotUuid;
    }

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    @Override
    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public void setSnapshotUuid(String snapshotUuid) {
        this.snapshotUuid = snapshotUuid;
    }

    public boolean isDbOnly() {
        return dbOnly;
    }

    public void setDbOnly(boolean dbOnly) {
        this.dbOnly = dbOnly;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }
}
