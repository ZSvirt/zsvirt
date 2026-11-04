package org.zstack.ha.fencers;

import static org.zstack.core.Platform.i18n;
import static org.zstack.ha.header.HaConstants.KVM_FENCER_CEPH;

public class CephKvmFencer implements KvmFencer {
    @Override
    public String getName() {
        return KVM_FENCER_CEPH;
    }

    @Override
    public String vmFencedReason(String fencerName) {
        return i18n("ceph storage is unavailable");
    }
}
