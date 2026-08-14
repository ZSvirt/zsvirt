package org.zstack.header.host;

import org.zstack.header.cluster.PowerOffHardwareMsg;

import java.util.List;

public interface PowerOffHostMessageBuilder {
    /**
     *
     * @param hostUuids
     * @return Customized power off hardware msg
     */
    List<PowerOffHardwareMsg> buildMsg(List<String> hostUuids);
}
