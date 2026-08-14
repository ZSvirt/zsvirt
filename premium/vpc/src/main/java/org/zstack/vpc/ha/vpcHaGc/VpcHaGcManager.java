package org.zstack.vpc.ha.vpcHaGc;

import org.zstack.header.core.Completion;

public interface VpcHaGcManager {
    void submitGc(VpcHaGcStruct gc, Completion completion);
}
