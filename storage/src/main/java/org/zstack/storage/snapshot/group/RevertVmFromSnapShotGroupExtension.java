package org.zstack.storage.snapshot.group;

import org.zstack.header.core.workflow.Flow;

public interface RevertVmFromSnapShotGroupExtension {
    boolean needRunExtension();
    Flow getBeforeRevertFlow();
}
