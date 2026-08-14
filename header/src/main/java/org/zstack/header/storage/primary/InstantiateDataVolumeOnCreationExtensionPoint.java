package org.zstack.header.storage.primary;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.volume.InstantiateVolumeMsg;
import org.zstack.header.volume.VolumeInventory;

/**
 * Created by xing5 on 2016/8/22.
 */
public interface InstantiateDataVolumeOnCreationExtensionPoint {
    String getPrimaryStorageTypeForInstantiateDataVolumeOnCreationExtensionPoint();

    void instantiateDataVolumeOnCreation(InstantiateVolumeMsg msg, VolumeInventory volume, ReturnValueCompletion<VolumeInventory> completion);
}
