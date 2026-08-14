package org.zstack.header.storage.addon.primary;

public class ActiveVolumeClient {
    protected String managerIp;
    protected String qualifiedName;
    protected boolean inBlacklist;
    protected String path;

    public void setManagerIp(String managerIp) {
        this.managerIp = managerIp;
    }

    public String getManagerIp() {
        return managerIp;
    }

    public void setQualifiedName(String qualifiedName) {
        this.qualifiedName = qualifiedName;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public void setInBlacklist(boolean inBlacklist) {
        this.inBlacklist = inBlacklist;
    }

    public boolean isInBlacklist() {
        return inBlacklist;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
