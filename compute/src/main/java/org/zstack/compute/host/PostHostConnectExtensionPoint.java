package org.zstack.compute.host;

import org.zstack.header.core.workflow.Flow;
import org.zstack.header.host.HostInventory;

import java.util.Map;

/**
 * Created by miao on 16-7-20.
 */
public interface PostHostConnectExtensionPoint {
    String KEY_INVENTORY = PreHostConnectExtensionPoint.KEY_INVENTORY;

    Flow createPostHostConnectFlow();

    default HostInventory findInventoryFromFlowData(Map<String, Object> flowData) {
        return (HostInventory) flowData.get(KEY_INVENTORY);
    }
}
