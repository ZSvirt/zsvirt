package org.zstack.header.storage.primary;

import java.util.List;

/**
 * Created by lining on 2019/4/16.
 */
public class CephPrimaryStorageAllocateConfig extends PrimaryStorageAllocateConfig {
    private List<String> poolNames;

    public List<String> getPoolNames() {
        return poolNames;
    }

    public void setPoolNames(List<String> poolNames) {
        this.poolNames = poolNames;
    }
}
