package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;

public class SetVolumeQosOnPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String primaryStorageUuid;
    private String volumeUuid;
    private Long totalBandWidth;
    private Long readBandwidth;
    private Long writeBandwidth;
    private Long readIOPS;
    private Long writeIOPS;
    private Long totalIOPS;

    // total, read, write, all, overwrite
    private String mode;

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public Long getTotalBandWidth() {
        return totalBandWidth;
    }

    public void setTotalBandWidth(Long totalBandWidth) {
        this.totalBandWidth = totalBandWidth;
    }

    public Long getReadBandwidth() {
        return readBandwidth;
    }

    public void setReadBandwidth(Long readBandwidth) {
        this.readBandwidth = readBandwidth;
    }

    public Long getWriteBandwidth() {
        return writeBandwidth;
    }

    public void setWriteBandwidth(Long writeBandwidth) {
        this.writeBandwidth = writeBandwidth;
    }

    public Long getReadIOPS() {
        return readIOPS;
    }

    public void setReadIOPS(Long readIOPS) {
        this.readIOPS = readIOPS;
    }

    public Long getWriteIOPS() {
        return writeIOPS;
    }

    public void setWriteIOPS(Long writeIOPS) {
        this.writeIOPS = writeIOPS;
    }

    public Long getTotalIOPS() {
        return totalIOPS;
    }

    public void setTotalIOPS(Long totalIOPS) {
        this.totalIOPS = totalIOPS;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}
