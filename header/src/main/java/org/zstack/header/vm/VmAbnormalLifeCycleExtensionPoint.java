package org.zstack.header.vm;

import org.zstack.header.core.workflow.Flow;

/**
 * Created by frank on 11/2/2015.
 */
public interface VmAbnormalLifeCycleExtensionPoint {
    Flow createVmAbnormalLifeCycleHandlingFlow(VmAbnormalLifeCycleStruct struct);
}
