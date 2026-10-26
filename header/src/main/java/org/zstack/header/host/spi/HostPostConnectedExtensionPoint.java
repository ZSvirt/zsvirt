package org.zstack.header.host.spi;

import org.zstack.header.core.workflow.Flow;
import org.zstack.header.host.HostInventory;

import java.util.Map;

/**
 * Named PostHostConnectExtensionPoint in version < 5.2.0
 *
 * @version zsv_5.2.0: update name to HostPostConnectedExtensionPoint, move to header module
 */
public interface HostPostConnectedExtensionPoint {
    String KEY_INVENTORY = HostPreConnectedExtensionPoint.KEY_INVENTORY;

    Flow createPostHostConnectFlow();

    default HostInventory findInventoryFromFlowData(Map<String, Object> flowData) {
        return (HostInventory) flowData.get(KEY_INVENTORY);
    }
}
