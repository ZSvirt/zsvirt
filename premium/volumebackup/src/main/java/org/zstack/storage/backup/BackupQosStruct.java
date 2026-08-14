package org.zstack.storage.backup;

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
}
