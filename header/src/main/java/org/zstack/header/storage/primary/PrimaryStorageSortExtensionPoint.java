package org.zstack.header.storage.primary;

import org.zstack.header.vm.VmInstanceSpec;

import java.util.List;

public interface PrimaryStorageSortExtensionPoint {
    void sort(List<PrimaryStorageVO> candidates, VmInstanceSpec.ImageSpec imageSpec, String allocateStrategy);
}