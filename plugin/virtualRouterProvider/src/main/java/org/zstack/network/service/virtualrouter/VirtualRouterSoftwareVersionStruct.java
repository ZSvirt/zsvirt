package org.zstack.network.service.virtualrouter;

public class VirtualRouterSoftwareVersionStruct {
    private String vrUuid;
    private String softwareName;
    private String currentVersion;
    private String latestVersion;

    public String getVrUuid() {
        return vrUuid;
    }

    public void setVrUuid(String vrUuid) {
        this.vrUuid = vrUuid;
    }

    public String getSoftwareName() {
        return softwareName;
    }

    public void setSoftwareName(String softwareName) {
        this.softwareName = softwareName;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public void setLatestVersion(String latestVersion) {
        this.latestVersion = latestVersion;
    }

}
