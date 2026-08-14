package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

import java.util.ArrayList;
import java.util.List;

public class DeleteVolumeChainOnPrimaryStorageReply extends MessageReply {
    private List<String> undeletedInstallPaths = new ArrayList<>();

    public List<String> getUndeletedInstallPaths() {
        return undeletedInstallPaths;
    }

    public void setUndeletedInstallPaths(List<String> undeletedInstallPaths) {
        this.undeletedInstallPaths = undeletedInstallPaths;
    }
}
