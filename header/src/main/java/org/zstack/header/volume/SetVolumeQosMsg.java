package org.zstack.header.volume;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by LiangHanYu on 2022/7/3 16:52
 */
public class SetVolumeQosMsg extends NeedReplyMessage implements VolumeMessage {
    private String uuid;

    private String mode = null;

    private Long volumeBandwidth;

    private Long readBandwidth;

    private Long writeBandwidth;

    private Long totalBandwidth;

    private Long readIOPS;

    private Long writeIOPS;

    private Long totalIOPS;

    private Integer version;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Long getVolumeBandwidth() {
        return volumeBandwidth;
    }

    public void setVolumeBandwidth(Long volumeBandwidth) {
        this.volumeBandwidth = volumeBandwidth;
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

    public Long getTotalBandwidth() {
        return totalBandwidth;
    }

    public void setTotalBandwidth(Long totalBandwidth) {
        this.totalBandwidth = totalBandwidth;
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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public String getVolumeUuid() {
        return uuid;
    }
}
