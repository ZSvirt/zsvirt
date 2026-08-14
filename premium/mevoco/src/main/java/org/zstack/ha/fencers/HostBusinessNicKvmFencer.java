package org.zstack.ha.fencers;

import org.zstack.header.tag.SystemTagInventory;

import static org.zstack.core.Platform.i18n;
import static org.zstack.ha.HaConstants.KVM_FENCER_HOST_BUSINESS_NIC;

public class HostBusinessNicKvmFencer implements KvmFencer {
    @Override
    public String getName() {
        return KVM_FENCER_HOST_BUSINESS_NIC;
    }

    @Override
    public String vmFencedReason(SystemTagInventory fencedByTag) {
        return i18n("host business NIC is down");
    }
}
