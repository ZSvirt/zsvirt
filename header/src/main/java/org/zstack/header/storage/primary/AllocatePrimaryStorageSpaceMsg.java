package org.zstack.header.storage.primary;

public class AllocatePrimaryStorageSpaceMsg extends AllocatePrimaryStorageMsg {
    private boolean force;
    private String requiredInstallUri;

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public String getRequiredInstallUri() {
        return requiredInstallUri;
    }

    public void setRequiredInstallUri(String requiredInstallUrl) {
        this.requiredInstallUri = requiredInstallUrl;
    }
}
