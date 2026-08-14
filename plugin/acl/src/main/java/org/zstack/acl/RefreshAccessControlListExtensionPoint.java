package org.zstack.acl;

import org.zstack.header.acl.AccessControlListEntryInventory;
import org.zstack.header.acl.AccessControlListInventory;

/**
 * @author: zhanyong.miao
 * @date: 2020-03-16
 **/
public interface RefreshAccessControlListExtensionPoint {
    void beforeAddIpEntry(AccessControlListInventory acl, AccessControlListEntryInventory entry);
    void afterAddIpEntry(AccessControlListInventory acl, AccessControlListEntryInventory entry);

    void beforeDeleteIpEntry(AccessControlListInventory acl, AccessControlListEntryInventory entry);
    void afterDeleteIpEntry(AccessControlListInventory acl, AccessControlListEntryInventory entry);

    void beforeDeleteAcl(AccessControlListInventory acl);
    void afterDeleteAcl(AccessControlListInventory acl);
}
