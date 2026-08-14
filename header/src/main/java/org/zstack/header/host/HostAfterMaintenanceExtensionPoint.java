package org.zstack.header.host;

import org.zstack.header.core.Completion;

/**
 * Create by weiwang at 2018/11/23
 */
public interface HostAfterMaintenanceExtensionPoint {
    void afterMaintenanceExtensionPoint(HostInventory hostInventory, Completion completion);
}
