package org.zstack.externalbackup;

import java.sql.Timestamp;

public abstract class ResourceExternalBackupInfo {
    protected String uuid;
    protected String name;
    protected ResourceBackupState state;
    protected String installPath;
    protected Timestamp createDate;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setState(ResourceBackupState state) {
        this.state = state;
    }

    public ResourceBackupState getState() {
        return state;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }
}
