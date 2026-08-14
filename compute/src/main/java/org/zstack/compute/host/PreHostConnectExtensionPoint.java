package org.zstack.compute.host;

import org.zstack.header.core.workflow.Flow;
import org.zstack.header.host.ConnectHostMsg;

/**
 * Created by GuoYi on 2019-06-03.
 */
public interface PreHostConnectExtensionPoint {
    Flow createPreHostConnectFlow(ConnectHostMsg message);
}
