package org.zstack.compute.host;

import org.zstack.header.core.Completion;
import org.zstack.header.host.HostPowerStatus;
import org.zstack.header.host.HostVO;

/**
 * @Author : jingwang
 * @create 2023/4/13 10:01 AM
 */
public interface HostPowerExecutor {
    void powerOff(HostVO host, Boolean force, Completion completion, boolean returnEarly);
    void powerOn(HostVO host, Completion completion);
    void powerReset(HostVO host, Completion completion, boolean returnEarly);
    HostPowerStatus getPowerStatus(HostVO host);
}
