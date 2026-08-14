package org.zstack.network.l3;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.network.l3.CheckIpAvailabilityResult;
import org.zstack.header.network.l3.CheckIpAvailabilityStruct;

public interface CheckIpAddressAvailabilityExtensionPoint {
    void check(CheckIpAvailabilityStruct struct, ReturnValueCompletion<CheckIpAvailabilityResult> completion);
}
