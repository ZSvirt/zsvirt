package org.zstack.kvm;

import org.zstack.header.core.Completion;
import org.zstack.header.host.HostInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;

import java.util.List;
import java.util.Map;

public interface KvmVmActiveVolumeSyncExtensionPoint {
    List<String> getStoragePathsForVolumeSync(HostInventory host, PrimaryStorageInventory attachedPs);
    void handleInactiveVolume(HostInventory host, Map<PrimaryStorageInventory, List<String>> inactiveVolumePaths, Completion completion);
}
