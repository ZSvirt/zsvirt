package org.zstack.network.service.virtualrouter.vyos;

import org.zstack.header.core.workflow.Flow;

/**
 * Created by shixin.ruan on 2021/03/22.
 */
public interface VyosProvisionConfigFlowExtensionPoint {
    Flow vyosProvisionConfigFlow();
}
