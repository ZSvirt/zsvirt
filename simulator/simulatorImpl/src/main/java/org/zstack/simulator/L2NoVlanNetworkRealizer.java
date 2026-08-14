package org.zstack.simulator;

import org.zstack.header.core.Completion;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l2.L2NetworkRealizationExtensionPoint;
import org.zstack.header.network.l2.L2NetworkType;
import org.zstack.header.simulator.SimulatorConstant;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class L2NoVlanNetworkRealizer implements L2NetworkRealizationExtensionPoint {
    private static final CLogger logger = Utils.getLogger(L2NoVlanNetworkRealizer.class);

    @Override
    public void realize(L2NetworkInventory l2Network, String hostUuid, Completion completion) {
        logger.debug(String.format("simulator successfully realized l2network[uuid:%s]", l2Network.getUuid()));
        completion.success();
    }

    @Override
    public void check(L2NetworkInventory l2Network, String hostUuid, Completion completion) {
        completion.success();
    }

    @Override
    public L2NetworkType getSupportedL2NetworkType() {
        return L2NetworkType.valueOf(L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE);
    }

    @Override
    public HypervisorType getSupportedHypervisorType() {
        return HypervisorType.valueOf(SimulatorConstant.SIMULATOR_HYPERVISOR_TYPE);
    }

    public void delete(L2NetworkInventory l2Network, String hostUuid, Completion completion) {
        completion.success();
    }

}
