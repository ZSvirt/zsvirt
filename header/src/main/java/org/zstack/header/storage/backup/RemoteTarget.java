package org.zstack.header.storage.backup;

public interface RemoteTarget {
    String getInstallPath();

    String getTargetUri();

    // for data network hostname
    String getTargetUri(String hostname);
}
