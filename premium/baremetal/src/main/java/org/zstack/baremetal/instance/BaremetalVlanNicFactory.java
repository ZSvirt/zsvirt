package org.zstack.baremetal.instance;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.Component;
import org.zstack.header.baremetal.network.*;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l2.L2VlanNetworkInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

public class BaremetalVlanNicFactory implements Component, BaremetalNicFactory {
    private static CLogger logger = Utils.getLogger(BaremetalVlanNicFactory.class);

    @Autowired
    protected DatabaseFacade dbf;

    @Override
    public String getType() {
        return L2NetworkConstant.L2_VLAN_NETWORK_TYPE;
    }

    @Override
    public BaremetalNic getBaremetalNic(BaremetalNicVO vo) {
        return new BaremetalVlanNic(vo);
    }

    @Override
    public BaremetalNicInventory createBaremetalNic(BaremetalNicVO vo, L2NetworkInventory l2) {
        BaremetalVlanNicVO vvo = new BaremetalVlanNicVO(vo);
        vvo.setVlan(((L2VlanNetworkInventory)l2).getVlan());
        vvo = dbf.persistAndRefresh(vvo);
        BaremetalNicInventory inv = vvo.toInventory();
        logger.debug("successfully create Baremetal VLAN Nic: " + JSONObjectUtil.toJsonString(inv));
        return inv;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
