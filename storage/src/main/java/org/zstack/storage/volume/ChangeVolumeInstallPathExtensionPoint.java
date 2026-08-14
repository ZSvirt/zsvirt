package org.zstack.storage.volume;

import org.zstack.header.core.Completion;
import org.zstack.header.vm.ChangeVmImageExtensionPoint;
import org.zstack.header.volume.VolumeInventory;

/**
 * Created by mingjian.deng on 2018/8/2.
 */
public interface ChangeVolumeInstallPathExtensionPoint extends ChangeVmImageExtensionPoint {
    void afterChangeVmVolumeInstallPath(String oldVolumeUuid, VolumeInventory newVol, final Completion completion);
}
