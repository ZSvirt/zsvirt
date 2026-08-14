package org.zstack.storage.primary.nfs;

import org.zstack.header.configuration.PythonClass;

@PythonClass
public interface NfsPrimaryStorageConstant {
    public static final String DEFAULT_NFS_MOUNT_PATH_ON_HOST = "/opt/zstack/nfsprimarystorage";
    @PythonClass
    public static final String NFS_PRIMARY_STORAGE_TYPE = "NFS";
}
