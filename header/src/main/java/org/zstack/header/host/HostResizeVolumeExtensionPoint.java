package org.zstack.header.host;

import org.zstack.header.volume.VolumeInventory;

/**
 * Created by kayo on 2018/4/2.
 */
public interface HostResizeVolumeExtensionPoint {
    HostResizeVolumeStruct beforeKvmHostResizeVolume(HostResizeVolumeStruct struct, VolumeInventory vol, String hostUuid);
}
