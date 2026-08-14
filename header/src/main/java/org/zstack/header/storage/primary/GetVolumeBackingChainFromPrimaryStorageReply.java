package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by MaJin on 2019/8/8.
 */
public class GetVolumeBackingChainFromPrimaryStorageReply extends MessageReply {
    private Map<String, List<String>> backingChainInstallPath = new HashMap<>();
    private Map<String, Long> backingChainSize = new HashMap<>();

    public Map<String, List<String>> getBackingChainInstallPath() {
        return backingChainInstallPath;
    }

    public void setBackingChainInstallPath(Map<String, List<String>> backingChainInstallPath) {
        this.backingChainInstallPath = backingChainInstallPath;
    }

    public List<String> getBackingChainInstallPath(String leafNodePath) {
        return backingChainInstallPath.get(leafNodePath);
    }

    public void putBackingChainInstallPath(String leafNodePath, List<String> backingChain) {
        this.backingChainInstallPath.put(leafNodePath, backingChain);
    }

    public Map<String, Long> getBackingChainSize() {
        return backingChainSize;
    }

    public void setBackingChainSize(Map<String, Long> backingChainSize) {
        this.backingChainSize = backingChainSize;
    }

    public void putBackingChainSize(String leafNodePath, long size) {
        this.backingChainSize.put(leafNodePath, size);
    }

    public Long getBackingChainSize(String leafNodePath) {
        return backingChainSize.get(leafNodePath);
    }
}
