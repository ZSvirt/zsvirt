package org.zstack.header.storage.addon.primary;

import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.host.HostInventory;

import java.util.List;

public interface PrimaryStorageNodeSvc {
    void activate(BaseVolumeInfo v, HostInventory h, boolean shareable, ReturnValueCompletion<ActiveVolumeTO> comp);

    void deactivate(String installPath, String protocol, HostInventory h, Completion comp);

    void deactivate(String installPath, String protocol, ActiveVolumeClient client, Completion comp);

    void blacklist(String installPath, String protocol, HostInventory h, Completion comp);

    String getActivePath(BaseVolumeInfo v, HostInventory h, boolean shareable);
    BaseVolumeInfo getActiveVolumeInfo(String activePath, HostInventory h, boolean shareable);

    // TODO use HostInventory
    List<ActiveVolumeClient> getActiveClients(String installPath, String protocol);

    List<String> getActiveVolumesLocation(HostInventory h);

    void deployClient(HostInventory h, Completion comp);

    void activateHeartbeatVolume(HostInventory h, ReturnValueCompletion<HeartbeatVolumeTO> comp);

    void deactivateHeartbeatVolume(HostInventory h, Completion comp);

    HeartbeatVolumeTO getHeartbeatVolumeActiveInfo(HostInventory h);

    String getIdentity();
}
