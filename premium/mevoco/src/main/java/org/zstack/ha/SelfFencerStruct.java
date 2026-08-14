package org.zstack.ha;

import org.zstack.header.host.HostInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;

import java.util.List;

/**
 * Created by xing5 on 2016/3/30.
 */
public class SelfFencerStruct {
    private HostInventory host;
    private List<PrimaryStorageInventory> primaryStorage;
    private String strategy = HaGlobalConfig.SELF_FENCER_STRATEGY.value();
    private List<String> fencers;

    public HostInventory getHost() {
        return host;
    }

    public void setHost(HostInventory host) {
        this.host = host;
    }

    public List<PrimaryStorageInventory> getPrimaryStorage() {
        return primaryStorage;
    }

    public void setPrimaryStorage(List<PrimaryStorageInventory> primaryStorage) {
        this.primaryStorage = primaryStorage;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public List<String> getFencers() {
        return fencers;
    }

    public void setFencers(List<String> fencers) {
        this.fencers = fencers;
    }
}
