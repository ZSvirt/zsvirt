package org.zstack.compute.vdpa;

import org.zstack.header.core.Completion;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.vdpa.GenerateVdpaMsg;
import org.zstack.header.vdpa.DeleteVdpasMsg;

public interface VmVdpaNicHypervisorBackend {
    HypervisorType getHypervisorType();

    void expungeVdpas(String hostUuid, DeleteVdpasMsg msg, Completion completion);

    void generateVdpa(String hostUuid, GenerateVdpaMsg msg, Completion completion);
}
