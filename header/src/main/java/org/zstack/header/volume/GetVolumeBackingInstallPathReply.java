package org.zstack.header.volume;

import org.zstack.header.message.MessageReply;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by MaJin on 2019/8/8.
 */
public class GetVolumeBackingInstallPathReply extends MessageReply {
    private List<String> currentBackingChain;
    private long currentBackingChainSize;
    private List<List<String>> previousBackingChains = new ArrayList<>();
    private List<Long> previousBackingChainSizes = new ArrayList<>();

    public List<String> getCurrentBackingChain() {
        return currentBackingChain;
    }

    public void setCurrentBackingChain(List<String> currentBackingChain) {
        this.currentBackingChain = currentBackingChain;
    }

    public long getCurrentBackingChainSize() {
        return currentBackingChainSize;
    }

    public void setCurrentBackingChainSize(long currentBackingChainSize) {
        this.currentBackingChainSize = currentBackingChainSize;
    }

    public List<List<String>> getPreviousBackingChains() {
        return previousBackingChains;
    }

    public void setPreviousBackingChains(List<List<String>> previousBackingChains) {
        this.previousBackingChains = previousBackingChains;
    }

    public void addPreviousBackingChain(List<String> chain) {
        this.previousBackingChains.add(chain);
    }

    public List<Long> getPreviousBackingChainSizes() {
        return previousBackingChainSizes;
    }

    public void setPreviousBackingChainSizes(List<Long> previousBackingChainSizes) {
        this.previousBackingChainSizes = previousBackingChainSizes;
    }

    public void addPreviousBackingChainSize(long size) {
        this.previousBackingChainSizes.add(size);
    }
}
