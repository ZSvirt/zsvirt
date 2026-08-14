package org.zstack.header.volume;

import org.zstack.utils.DebugUtils;

import static org.zstack.header.volume.VolumeQosConstant.*;

public class VolumeQos {
    private Long readBandwidth = -1L;
    private Long writeBandwidth = -1L;
    private Long totalBandwidth = -1L;
    private Long readIOPS = -1L;
    private Long writeIOPS = -1L;
    private Long totalIOPS = -1L;

    public VolumeQos(Long readBandwidth, Long writeBandwidth, Long totalBandwidth, Long readIOPS, Long writeIOPS, Long totalIOPS) {
        this.readBandwidth = readBandwidth != null ? readBandwidth : -1L;
        this.writeBandwidth = writeBandwidth != null ? writeBandwidth : -1L;
        this.totalBandwidth = totalBandwidth != null ? totalBandwidth : -1L;
        this.readIOPS = readIOPS != null ? readIOPS : -1L;
        this.writeIOPS = writeIOPS != null ? writeIOPS : -1L;
        this.totalIOPS = totalIOPS != null ? totalIOPS : -1L;
    }

    public VolumeQos() {
    }

    public static VolumeQos valueOf(String volumeQos) {
        VolumeQos qos = new VolumeQos();
        if (volumeQos == null) {
            return qos;
        }
        String[] vqoss = volumeQos.split(",");
        for (String vqos: vqoss) {
            String[] v = vqos.trim().split("=");
            DebugUtils.Assert(v.length == 2,
                    String.format("qos must be formatted as read=xxxx,write=xxx, but got %s", volumeQos)
            );

            if (v[0] == null || v[1] == null) {
                continue;
            }
            switch (v[0]) {
                case BANDWIDTH_TOTAL:
                    qos.setTotalBandwidth(Long.parseLong(v[1]));
                    break;
                case BANDWIDTH_READ:
                    qos.setReadBandwidth(Long.parseLong(v[1]));
                    break;
                case BANDWIDTH_WRITE:
                    qos.setWriteBandwidth(Long.parseLong(v[1]));
                    break;
                case IOPS_TOTAL:
                    qos.setTotalIOPS(Long.parseLong(v[1]));
                    break;
                case IOPS_READ:
                    qos.setReadIOPS(Long.parseLong(v[1]));
                    break;
                case IOPS_WRITE:
                    qos.setWriteIOPS(Long.parseLong(v[1]));
                    break;
            }
        }

        return qos;
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

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VolumeQos) {
            VolumeQos other = (VolumeQos) obj;
            return (other.readBandwidth.longValue() == readBandwidth.longValue())
                    && (other.writeBandwidth.longValue() == writeBandwidth.longValue())
                    && (other.totalBandwidth.longValue() == totalBandwidth.longValue())
                    && (other.totalIOPS.longValue() == totalIOPS.longValue())
                    && (other.readIOPS.longValue() == readIOPS.longValue())
                    && (other.writeIOPS.longValue() == readIOPS.longValue());
        } else {
            return super.equals(obj);
        }
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
