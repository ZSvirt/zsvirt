package org.zstack.header.vm;

import org.zstack.header.errorcode.ErrorCode;

/**
 * Created by frank on 7/18/2015.
 */
public interface VmDetachNicExtensionPoint {
    void preDetachNic(VmNicInventory nic);

    void beforeDetachNic(VmNicInventory nic);

    void afterDetachNic(VmNicInventory nic);

    void failedToDetachNic(VmNicInventory nic, ErrorCode error);
}
