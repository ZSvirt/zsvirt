package org.zstack.storage.backup;

import java.util.Objects;

/**
 * Created by kayo on 2018/10/10.
 */
public class BackupQosStruct {
    private Long volumeReadBandwidth;
    private Long volumeWriteBandwidth;
    private Long networkReadBandwidth;
    private Long networkWriteBandwidth;

    public Long getVolumeReadBandwidth() {
        return volumeReadBandwidth;
    }

    public void setVolumeReadBandwidth(Long volumeReadBandwidth) {
        this.volumeReadBandwidth = volumeReadBandwidth;
    }

    public Long getVolumeWriteBandwidth() {
        return volumeWriteBandwidth;
    }

    public void setVolumeWriteBandwidth(Long volumeWriteBandwidth) {
        this.volumeWriteBandwidth = volumeWriteBandwidth;
    }

    public Long getNetworkReadBandwidth() {
        return networkReadBandwidth;
    }

    public void setNetworkReadBandwidth(Long networkReadBandwidth) {
        this.networkReadBandwidth = networkReadBandwidth;
    }

    public Long getNetworkWriteBandwidth() {
        return networkWriteBandwidth;
    }

    public void setNetworkWriteBandwidth(Long networkWriteBandwidth) {
        this.networkWriteBandwidth = networkWriteBandwidth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BackupQosStruct that = (BackupQosStruct) o;
        return Objects.equals(volumeReadBandwidth, that.volumeReadBandwidth) &&
                Objects.equals(volumeWriteBandwidth, that.volumeWriteBandwidth) &&
                Objects.equals(networkReadBandwidth, that.networkReadBandwidth) &&
                Objects.equals(networkWriteBandwidth, that.networkWriteBandwidth);
    }

    @Override
    public int hashCode() {
        return Objects.hash(volumeReadBandwidth, volumeWriteBandwidth, networkReadBandwidth, networkWriteBandwidth);
    }
}
