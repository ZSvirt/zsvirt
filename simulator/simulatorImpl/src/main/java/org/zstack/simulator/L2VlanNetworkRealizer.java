package org.zstack.simulator;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.network.l2.*;
import org.zstack.header.simulator.SimulatorConstant;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class L2VlanNetworkRealizer implements L2NetworkRealizationExtensionPoint {
    private static CLogger logger = Utils.getLogger(L2VlanNetworkRealizer.class);

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void realize(L2NetworkInventory l2Network, String hostUuid, Completion completion) {
        L2VlanNetworkVO vo = dbf.findByUuid(l2Network.getUuid(), L2VlanNetworkVO.class);
        logger.debug(String.format("simulator successfully realized l2network[uuid:%s, vlan:%s]", l2Network.getUuid(), vo.getVlan()));
        completion.success();
    }

    @Override
    public void check(L2NetworkInventory l2Network, String hostUuid, Completion completion) {
        completion.success();
    }

    @Override
    public L2NetworkType getSupportedL2NetworkType() {
        return L2NetworkType.valueOf(L2NetworkConstant.L2_VLAN_NETWORK_TYPE);
    }

    @Override
    public HypervisorType getSupportedHypervisorType() {
        return HypervisorType.valueOf(SimulatorConstant.SIMULATOR_HYPERVISOR_TYPE);
    }

    public void delete(L2NetworkInventory l2Network, String hostUuid, Completion completion) {
        completion.success();
    }

}
