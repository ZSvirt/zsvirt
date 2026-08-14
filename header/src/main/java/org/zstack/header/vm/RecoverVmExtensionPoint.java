package org.zstack.header.vm;

/**
 * Created by frank on 11/24/2015.
 */
public interface RecoverVmExtensionPoint {
    void preRecoverVm(VmInstanceInventory vm);

    void beforeRecoverVm(VmInstanceInventory vm);

    void afterRecoverVm(VmInstanceInventory vm);
}
