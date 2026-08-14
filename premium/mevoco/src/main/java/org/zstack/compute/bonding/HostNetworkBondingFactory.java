package org.zstack.compute.bonding;

import org.zstack.header.core.Completion;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingInventory;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingType;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceInventory;

import java.util.List;

public interface HostNetworkBondingFactory {
    HostNetworkBondingType getType();

    void createBonding(HostNetworkBondingInventory bondingInv, Completion completion);

    void updateBonding(HostNetworkBondingInventory preBondingInv, HostNetworkBondingInventory bondingInv, Completion completion);

    void deleteBonding(HostNetworkBondingInventory bondingInv, Completion completion);

    void attachNicToBonding(HostNetworkBondingInventory bondingInv, List<HostNetworkInterfaceInventory> slaves, Completion completion);

    void detachNicFromBonding(HostNetworkBondingInventory bondingInv, List<HostNetworkInterfaceInventory> slaves, Completion completion);
}
