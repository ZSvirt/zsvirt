package org.zstack.header.host.spi;

import org.zstack.header.core.workflow.Flow;
import org.zstack.header.host.ConnectHostInfo;
import org.zstack.header.host.HostInventory;

import java.util.Map;

/**
 * Named PreHostConnectExtensionPoint in version < 5.2.0
 *
 * @version zsv_5.2.0: update name to HostPreConnectedExtensionPoint, move to header module
 */
public interface HostPreConnectedExtensionPoint {
    String KEY_INVENTORY = "HOST_INVENTORY";
    String KEY_CONNECT_HOST_INFO = "CONNECT_HOST_INFO";

    Flow createPreHostConnectFlow();

    default HostInventory findInventoryFromFlowData(Map<String, Object> flowData) {
        return (HostInventory) flowData.get(KEY_INVENTORY);
    }

    default ConnectHostInfo findConnectHostInfoFromFlowData(Map<String, Object> flowData) {
        return (ConnectHostInfo) flowData.get(KEY_CONNECT_HOST_INFO);
    }
}
