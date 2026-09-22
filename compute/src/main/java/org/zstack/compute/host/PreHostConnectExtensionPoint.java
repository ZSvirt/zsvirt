package org.zstack.compute.host;

import org.zstack.header.core.workflow.Flow;
import org.zstack.header.host.ConnectHostInfo;
import org.zstack.header.host.HostInventory;

import java.util.Map;

/**
 * Created by GuoYi on 2019-06-03.
 */
public interface PreHostConnectExtensionPoint {
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
