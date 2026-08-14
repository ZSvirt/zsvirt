package org.zstack.baremetal.instance;

import org.zstack.header.baremetal.network.BaremetalNicInventory;
import org.zstack.header.baremetal.network.BaremetalNicVO;
import org.zstack.header.baremetal.network.BaremetalVlanNicInventory;
import org.zstack.header.baremetal.network.BaremetalVlanNicVO;

public class BaremetalVlanNic extends BaremetalNoVlanNic {
    public BaremetalVlanNic() {
    }

    public BaremetalVlanNic(BaremetalNicVO self) {
        super(self);
    }

    private BaremetalVlanNicVO getSelf() {
        return (BaremetalVlanNicVO) self;
    }

    @Override
    protected BaremetalNicInventory getSelfInventory() {
        return BaremetalVlanNicInventory.valueOf(getSelf());
    }
}
