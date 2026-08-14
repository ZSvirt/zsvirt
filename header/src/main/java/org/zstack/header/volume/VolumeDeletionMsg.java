package org.zstack.header.volume;

import org.zstack.header.message.DeletionMessage;

/**
 */
public class VolumeDeletionMsg extends DeletionMessage implements VolumeMessage {
    private String volumeUuid;
    private boolean detachBeforeDeleting;
    private String deletionPolicy;

    public String getDeletionPolicy() {
        return deletionPolicy;
    }

    public void setDeletionPolicy(String deletionPolicy) {
        this.deletionPolicy = deletionPolicy;
    }

    public boolean isDetachBeforeDeleting() {
        return detachBeforeDeleting;
    }

    public void setDetachBeforeDeleting(boolean detachBeforeDeleting) {
        this.detachBeforeDeleting = detachBeforeDeleting;
    }

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
}
