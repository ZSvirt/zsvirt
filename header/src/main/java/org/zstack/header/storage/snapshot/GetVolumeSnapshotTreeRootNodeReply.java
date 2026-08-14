package org.zstack.header.storage.snapshot;

import org.zstack.header.message.MessageReply;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by MaJin on 2019/8/8.
 */
public class GetVolumeSnapshotTreeRootNodeReply extends MessageReply {
    private String currentRootInstallPath;
    private List<String> previousRootInstallPaths = new ArrayList<>();

    public String getCurrentRootInstallPath() {
        return currentRootInstallPath;
    }

    public void setCurrentRootInstallPath(String currentRootInstallPath) {
        this.currentRootInstallPath = currentRootInstallPath;
    }

    public List<String> getPreviousRootInstallPaths() {
        return previousRootInstallPaths;
    }

    public void setPreviousRootInstallPaths(List<String> previousRootInstallPaths) {
        this.previousRootInstallPaths = previousRootInstallPaths;
    }

    public void addPreviousRootInstallPath(String previousRootInstallPath) {
        this.previousRootInstallPaths.add(previousRootInstallPath);
    }
}
