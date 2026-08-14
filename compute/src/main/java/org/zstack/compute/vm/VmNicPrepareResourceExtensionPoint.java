package org.zstack.compute.vm;

import org.zstack.header.core.workflow.Flow;

public interface VmNicPrepareResourceExtensionPoint {
    Flow getPreparationFlow();
}
