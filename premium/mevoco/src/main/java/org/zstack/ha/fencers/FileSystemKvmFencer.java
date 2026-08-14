package org.zstack.ha.fencers;

import org.zstack.header.tag.SystemTagInventory;

import static org.zstack.core.Platform.i18n;
import static org.zstack.ha.HaConstants.KVM_FENCER_FILE_SYSTEM;

public class FileSystemKvmFencer implements KvmFencer {
    @Override
    public String getName() {
        return KVM_FENCER_FILE_SYSTEM;
    }

    @Override
    public String vmFencedReason(SystemTagInventory fencedByTag) {
        return i18n("host file system is disconnected");
    }
}
