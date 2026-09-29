package org.zstack.ha.fencers;

import static org.zstack.core.Platform.i18n;
import static org.zstack.ha.header.HaConstants.KVM_FENCER_HOST_BUSINESS_NIC;

public class HostBusinessNicKvmFencer implements KvmFencer {
    @Override
    public String getName() {
        return KVM_FENCER_HOST_BUSINESS_NIC;
    }

    @Override
    public String vmFencedReason(String fencerName) {
        return i18n("host business NIC is down");
    }
}
