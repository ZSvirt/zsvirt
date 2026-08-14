package org.zstack.externalbackup;

public abstract class ExternalBackupMetadata {
    protected String version;

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}
