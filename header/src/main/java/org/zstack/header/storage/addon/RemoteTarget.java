package org.zstack.header.storage.addon;

public interface RemoteTarget {
    // unique identifier - how to identify image/volume.
    String getInstallPath();

    // URI to access the image/volume, e.g.:
    //  - sftp://host/path/to/file
    //  - nbd://host/export-name
    //  - rbd://pool/image
    //  - nvme://host-nqn@ip:port/nqn/diskId
    String getResourceURI();
}
