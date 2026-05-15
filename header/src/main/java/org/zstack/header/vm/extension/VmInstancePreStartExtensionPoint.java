package org.zstack.header.vm.extension;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceInventory;

public interface VmInstancePreStartExtensionPoint {
    /**
     * @return null if the VM may start; otherwise a failure {@link ErrorCode} that blocks start.
     */
    ErrorCode preStartVm(VmInstanceInventory inv);
}
