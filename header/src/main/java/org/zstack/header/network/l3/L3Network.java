package org.zstack.header.network.l3;

import org.zstack.header.message.Message;

public interface L3Network {
    void handleMessage(Message msg);

    void deleteHook();

    CheckIpAvailabilityResult checkIpAvailability(CheckIpAvailabilityStruct struct);
}
