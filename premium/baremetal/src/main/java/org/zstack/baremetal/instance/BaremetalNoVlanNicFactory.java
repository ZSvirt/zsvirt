package org.zstack.baremetal.instance;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.Component;
import org.zstack.header.baremetal.network.BaremetalNic;
import org.zstack.header.baremetal.network.BaremetalNicFactory;
import org.zstack.header.baremetal.network.BaremetalNicInventory;
import org.zstack.header.baremetal.network.BaremetalNicVO;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

public class BaremetalNoVlanNicFactory implements Component, BaremetalNicFactory {
    private static CLogger logger = Utils.getLogger(BaremetalNoVlanNicFactory.class);

    @Autowired
    protected DatabaseFacade dbf;

    @Override
    public String getType() {
        return L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE;
    }

    @Override
    public BaremetalNic getBaremetalNic(BaremetalNicVO vo) {
        return new BaremetalNoVlanNic(vo);
    }

    @Override
    public BaremetalNicInventory createBaremetalNic(BaremetalNicVO vo, L2NetworkInventory l2) {
        vo = dbf.persistAndRefresh(vo);
        BaremetalNicInventory inv = vo.toInventory();
        logger.info("successfully create Baremetal NOVLAN Nic: %s" + JSONObjectUtil.toJsonString(inv));
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
