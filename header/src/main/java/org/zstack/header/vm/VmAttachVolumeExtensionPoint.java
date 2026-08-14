package org.zstack.header.vm;

import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.volume.VolumeInventory;

import java.util.Map;

/**
 * Created by frank on 6/10/2015.
 */
public interface VmAttachVolumeExtensionPoint {
    default void preAttachVolume(VmInstanceInventory vm, VolumeInventory volume) {}

    default void preAttachVolume(VmInstanceInventory vm, VolumeInventory volume, Completion completion) {
        preAttachVolume(vm, volume);
        completion.success();
    }

    void beforeAttachVolume(VmInstanceInventory vm, VolumeInventory volume, Map data);

    default void afterInstantiateVolume(VmInstanceInventory vm, VolumeInventory volume) {}

    default void afterInstantiateVolume(VmInstanceInventory vm, VolumeInventory volume, Completion completion) {
        afterInstantiateVolume(vm, volume);
        completion.success();
    }

    default void afterInstantiateVolumeForNewCreatedVm(VmInstanceInventory vm, VolumeInventory volume) {}

    void afterAttachVolume(VmInstanceInventory vm, VolumeInventory volume);

    void failedToAttachVolume(VmInstanceInventory vm, VolumeInventory volume, ErrorCode errorCode, Map data);
}
