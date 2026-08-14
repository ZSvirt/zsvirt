package org.zstack.network.service.virtualrouter.vyos;

import org.zstack.header.core.workflow.Flow;

/**
 * Created by shixin.ruan on 2019/11/6.
 */
public interface VyosPostMigrateFlowExtensionPoint {
    Flow vyosPostMigrateFlow();
}
