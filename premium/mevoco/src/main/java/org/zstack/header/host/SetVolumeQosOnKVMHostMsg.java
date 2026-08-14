package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeQos;

/**
 * Created by mingjian.deng on 17/1/10.
 */
public class SetVolumeQosOnKVMHostMsg extends NeedReplyMessage implements HostMessage {
    private VolumeInventory volume;
    private String hostUuid;
    private String vmUuid;
    private long totalBandWidth = 0;
    private String installPath;
    private long readBandwidth = 0;
    private long writeBandwidth = 0;
    // total, read, write, all, overwrite
    private String mode;
    private long readIOPS = 0;
    private long writeIOPS = 0;
    private long totalIOPS = 0;

    public void setMsgQos(VolumeQos qos) {
        if (qos == null) {
            return;
        }
        if (qos.getTotalBandwidth() != -1L) {
            totalBandWidth = qos.getTotalBandwidth();
        }
        if (qos.getReadBandwidth() != -1L) {
            readBandwidth = qos.getReadBandwidth();
        }
        if (qos.getWriteBandwidth() != -1L) {
            writeBandwidth = qos.getWriteBandwidth();
        }
        if (qos.getTotalIOPS() != -1L) {
            totalIOPS = qos.getTotalIOPS();
        }
        if (qos.getReadIOPS() != -1L) {
            readIOPS = qos.getReadIOPS();
        }
        if (qos.getWriteIOPS() != -1L) {
            writeIOPS = qos.getWriteIOPS();
        }
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public long getTotalBandWidth() {
        return totalBandWidth;
    }

    public void setTotalBandWidth(long totalBandWidth) {
        this.totalBandWidth = totalBandWidth;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public VolumeInventory getVolume() {
        return volume;
    }

    public void setVolume(VolumeInventory volume) {
        this.volume = volume;
    }

    public long getReadBandwidth() {
        return readBandwidth;
    }

    public void setReadBandwidth(long readBandwidth) {
        this.readBandwidth = readBandwidth;
    }

    public long getWriteBandwidth() {
        return writeBandwidth;
    }

    public void setWriteBandwidth(long writeBandwidth) {
        this.writeBandwidth = writeBandwidth;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public long getReadIOPS() {
        return readIOPS;
    }

    public void setReadIOPS(long readIOPS) {
        this.readIOPS = readIOPS;
    }

    public long getWriteIOPS() {
        return writeIOPS;
    }

    public void setWriteIOPS(long writeIOPS) {
        this.writeIOPS = writeIOPS;
    }

    public long getTotalIOPS() {
        return totalIOPS;
    }

    public void setTotalIOPS(long totalIOPS) {
        this.totalIOPS = totalIOPS;
    }
}
