package org.zstack.vpc.ha.vyos;

import org.zstack.header.core.Completion;

public interface vyosVpcHaRouterBackendManager {
    void enableHa(String vrUuid, Long timeout, Completion completion);
    void enableHa(String vrUuid, Completion completion);
}
