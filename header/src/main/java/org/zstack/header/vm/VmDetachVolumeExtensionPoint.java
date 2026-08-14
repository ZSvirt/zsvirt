package org.zstack.header.vm;

import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.volume.VolumeInventory;

/**
 * Created by frank on 6/10/2015.
 */
public interface VmDetachVolumeExtensionPoint {
    default void preDetachVolume(VmInstanceInventory vm, VolumeInventory volume, Completion completion) {
        completion.success();
    }

    default void beforeDetachVolume(VmInstanceInventory vm, VolumeInventory volume){}

    void afterDetachVolume(VmInstanceInventory vm, VolumeInventory volume, Completion completion);

    void failedToDetachVolume(VmInstanceInventory vm, VolumeInventory volume, ErrorCode errorCode);
}
