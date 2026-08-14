package org.zstack.compute.bonding;

import org.zstack.header.core.Completion;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingInventory;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingType;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

public class HostNetworkOvsBondingFactory implements HostNetworkBondingFactory {
    protected static final CLogger logger = Utils.getLogger(HostNetworkOvsBondingFactory.class);
    private static HostNetworkBondingType type = new HostNetworkBondingType(HostNetworkBondingConstant.OVS_BONDING_TYPE);

    @Override
    public HostNetworkBondingType getType() {
        return type;
    }

    @Override
    public void createBonding(HostNetworkBondingInventory bondingInv, Completion completion) {

    }

    @Override
    public void updateBonding(HostNetworkBondingInventory preBondingInv, HostNetworkBondingInventory bondingInv, Completion completion) {

    }

    @Override
    public void deleteBonding(HostNetworkBondingInventory bondingInv, Completion completion) {

    }

    @Override
    public void attachNicToBonding(HostNetworkBondingInventory bondingInv, List<HostNetworkInterfaceInventory> slaves, Completion completion) {

    }

    @Override
    public void detachNicFromBonding(HostNetworkBondingInventory bondingInv, List<HostNetworkInterfaceInventory> slaves, Completion completion) {

    }
}
