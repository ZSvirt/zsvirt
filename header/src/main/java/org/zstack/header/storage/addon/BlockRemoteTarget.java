package org.zstack.header.storage.addon;

public class BlockRemoteTarget implements RemoteTarget {
    protected String installPath;

    @Override
    public String getInstallPath() {
        return installPath;
    }

    @Override
    public String getResourceURI() {
        return null;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }
}
