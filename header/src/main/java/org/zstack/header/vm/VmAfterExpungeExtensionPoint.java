package org.zstack.header.vm;


/**
 * Created by Mei Lei on 8/23/16.
 */
public interface VmAfterExpungeExtensionPoint {
    void vmAfterExpunge(VmInstanceInventory inv);
}
