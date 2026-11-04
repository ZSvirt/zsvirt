package org.zstack.ha.message;

import org.zstack.header.host.HostInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;

import java.util.List;

/**
 * Created by xing5 on 2016/3/30.
 */
public class SelfFencerStruct {
    public HostInventory host;
    public List<PrimaryStorageInventory> primaryStorages;
    public String strategy;
    public List<String> fencers;
    public boolean skipStatusCheck; // "noStatusCheck" in cloud
}
