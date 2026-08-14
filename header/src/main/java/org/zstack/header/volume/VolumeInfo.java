package org.zstack.header.volume;

public class VolumeInfo {
    private String installPath;
    private Long actualSize;
    private Long size;

    public VolumeInfo(String installPath, Long actualSize, Long size) {
        this.installPath = installPath;
        this.actualSize = actualSize;
        this.size = size;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public Long getActualSize() {
        return actualSize;
    }

    public void setActualSize(Long actualSize) {
        this.actualSize = actualSize;
    }
}
