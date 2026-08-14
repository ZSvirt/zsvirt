package org.zstack.header.volume;

public class VolumeStats {
    protected String installPath;
    protected String format;
    protected Long actualSize;
    protected Long size;
    /**
     * The parent uri of the volume, vendor://pool/path@snapshot or snapshot://uuid
     */
    protected String parentUri;

    // TODO(shenjin): remove it
    @Deprecated
    protected String runStatus;

    public VolumeStats(String installPath, Long actualSize) {
        this.installPath = installPath;
        this.actualSize = actualSize;
    }


    public VolumeStats(String installPath, Long actualSize, Long size) {
        this.installPath = installPath;
        this.actualSize = actualSize;
        this.size = size;
    }

    public VolumeStats() {
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

    public Long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getFormat() {
        return format;
    }

    public void setParentUri(String parentUri) {
        this.parentUri = parentUri;
    }

    public String getParentUri() {
        return parentUri;
    }

    @Deprecated
    public String getRunStatus() {
        return runStatus;
    }

    public void setRunStatus(String runStatus) {
        this.runStatus = runStatus;
    }
}
