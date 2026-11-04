package org.zstack.ha.fencers;

import static org.zstack.core.Platform.i18n;
import static org.zstack.ha.header.HaConstants.KVM_FENCER_FILE_SYSTEM;

public class FileSystemKvmFencer implements KvmFencer {
    @Override
    public String getName() {
        return KVM_FENCER_FILE_SYSTEM;
    }

    @Override
    public String vmFencedReason(String fencerName) {
        return i18n("host file system is disconnected");
    }
}
