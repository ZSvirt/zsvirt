package org.zstack.header.host;

import org.zstack.header.volume.VolumeInventory;

/**
 * Created by mingjian.deng on 2018/10/22.
 */
public interface KVMHostSetVolumeQosExtensionPoint {
    boolean forbbidenSetVolumeQos(VolumeInventory vol);
}
