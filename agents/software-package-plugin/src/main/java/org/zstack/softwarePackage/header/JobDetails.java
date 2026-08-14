package org.zstack.softwarePackage.header;

/**
 * This is a View class (like AccountGroupResourceView as an API reply inventory)
 */
public class JobDetails {
    private String longJobUuid;
    private String longJobState;
    private String softwarePackageUuid;
    private String softwarePackageUploadUrl;
    private long offset;

    public String getLongJobUuid() {
        return longJobUuid;
    }

    public void setLongJobUuid(String longJobUuid) {
        this.longJobUuid = longJobUuid;
    }

    public String getLongJobState() {
        return longJobState;
    }

    public void setLongJobState(String longJobState) {
        this.longJobState = longJobState;
    }

    public String getSoftwarePackageUploadUrl() {
        return softwarePackageUploadUrl;
    }

    public void setSoftwarePackageUploadUrl(String softwarePackageUploadUrl) {
        this.softwarePackageUploadUrl = softwarePackageUploadUrl;
    }

    public String getSoftwarePackageUuid() {
        return softwarePackageUuid;
    }

    public void setSoftwarePackageUuid(String softwarePackageUuid) {
        this.softwarePackageUuid = softwarePackageUuid;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }
}
