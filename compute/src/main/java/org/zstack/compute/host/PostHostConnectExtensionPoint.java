package org.zstack.compute.host;

import org.zstack.header.core.workflow.Flow;
import org.zstack.header.host.HostInventory;

/**
 * Created by miao on 16-7-20.
 */
public interface PostHostConnectExtensionPoint {
    Flow createPostHostConnectFlow(HostInventory host);
}
