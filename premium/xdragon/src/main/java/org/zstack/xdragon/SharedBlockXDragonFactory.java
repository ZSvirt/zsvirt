package org.zstack.xdragon;

import org.zstack.storage.primary.sharedblock.SharedBlockKvmFactory;

public class SharedBlockXDragonFactory extends SharedBlockKvmFactory {
    @Override
    public String getHypervisorType() {
        return XDragonConstant.HYPERVISOR_TYPE;
    }
}
