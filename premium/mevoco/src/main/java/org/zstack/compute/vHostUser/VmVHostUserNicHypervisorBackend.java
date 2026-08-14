package org.zstack.compute.vHostUser;

import org.zstack.header.core.Completion;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.vHostUser.DeleteVHostUserResourceMsg;
import org.zstack.header.vHostUser.GenerateVHostUserResourceMsg;

public interface VmVHostUserNicHypervisorBackend {
    HypervisorType getHypervisorType();

    void expungeVHostUserResource(String hostUuid, DeleteVHostUserResourceMsg msg, Completion completion);

    void generateVHostUserResource(String hostUuid, GenerateVHostUserResourceMsg msg, Completion completion);
}
