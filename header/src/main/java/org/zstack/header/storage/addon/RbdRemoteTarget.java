package org.zstack.header.storage.addon;

public class RbdRemoteTarget implements RemoteTarget {
    private String installPath;

    @Override
    public String getInstallPath() {
        return installPath;
    }

    @Override
    public String getResourceURI() {
        return null;
    }
}
