package org.zstack.header.vm;

public interface ApplianceVmInstanceCreateExtensionPoint {
    default void afterPersistApplianceVmInstanceVO(VmInstanceVO vo) {}
}
