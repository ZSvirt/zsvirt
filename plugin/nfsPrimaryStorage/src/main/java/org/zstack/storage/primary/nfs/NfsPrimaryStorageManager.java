package org.zstack.storage.primary.nfs;

import org.zstack.header.host.HypervisorType;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackendCommands.NfsPrimaryStorageAgentResponse;

/**
 */
public interface NfsPrimaryStorageManager {
    void reportCapacityIfNeeded(String psUuid, NfsPrimaryStorageAgentResponse rsp);

    HypervisorType findHypervisorTypeByImageFormatAndPrimaryStorageUuid(String imageFormat, String psUuid);
}
