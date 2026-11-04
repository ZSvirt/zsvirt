package org.zstack.header.vm;

import org.zstack.header.errorcode.ErrorCode;

/**
 * Created by lining on 2017/7/6.
 */
public interface VmHaExtensionPoint {
    /**
     * If you need to prevent the VM from executing HA,
     * return {@link ErrorCode}.
     *
     * For example, this VM is strongly bound to its current host
     * (maybe Pci device attached) and cannot be migrated.
     */
    ErrorCode preHaStartVm(String vmUuid);
}
