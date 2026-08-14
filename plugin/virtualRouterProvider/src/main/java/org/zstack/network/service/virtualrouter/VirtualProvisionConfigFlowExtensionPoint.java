package org.zstack.network.service.virtualrouter;

import org.zstack.header.core.workflow.Flow;

/**
 * Created by shixin.ruan 2021/03/19
 */
public interface VirtualProvisionConfigFlowExtensionPoint {
    Flow provisionConfigFlow();
}
