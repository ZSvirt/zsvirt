package org.zstack.header.imagestore;

import java.util.List;

public interface ImageStoreReclaimSpaceExtensionPoint {
    List<String> getAvailableImageInstallPaths(String backupStorageUuid);
}
