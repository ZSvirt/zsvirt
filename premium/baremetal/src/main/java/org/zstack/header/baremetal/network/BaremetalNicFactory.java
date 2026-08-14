package org.zstack.header.baremetal.network;

import org.zstack.header.network.l2.L2NetworkInventory;

public interface BaremetalNicFactory {
    String getType();
    BaremetalNic getBaremetalNic(BaremetalNicVO vo);
    BaremetalNicInventory createBaremetalNic(BaremetalNicVO vo, L2NetworkInventory l2);
}
