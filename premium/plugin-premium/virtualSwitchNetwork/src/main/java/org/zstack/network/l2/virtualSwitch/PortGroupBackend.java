package org.zstack.network.l2.virtualSwitch;

import org.zstack.header.core.Completion;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.network.l2.virtualSwitch.header.APICreatePortGroupMsg;
import org.zstack.network.l2.virtualSwitch.header.L2PortGroupNetworkInventory;

import static org.zstack.core.Platform.argerr;

public interface PortGroupBackend {
    boolean supports(String vSwitchUuid);

    default KVMAgentCommands.NicTO completeNicInformation(L2NetworkInventory l2, L3NetworkInventory l3, VmNicInventory nic) {
        return null;
    }

    default String getBridgeName() {
        return null;
    }

    default void validateCreate(APICreatePortGroupMsg msg) {
    }

    default boolean requiresLocalClusterBinding() {
        return true;
    }

    void create(L2PortGroupNetworkInventory inventory, Completion completion);

    default void create(L2PortGroupNetworkInventory inventory, String tenantRouterUuid, Completion completion) {
        create(inventory, completion);
    }


    default void afterLocalCreate(L2PortGroupNetworkInventory inventory, Completion completion) {
        completion.success();
    }

    void updateVirtualNetworkId(L2PortGroupNetworkInventory inventory, Integer virtualNetworkId,
                                Completion completion);

    default void updateTenantRouter(L2PortGroupNetworkInventory inventory, String tenantRouterUuid,
                                    Completion completion) {
        completion.fail(argerr("tenantRouterUuid requires a supported ZNS port group"));
    }

    default void beforeLocalDelete(L2PortGroupNetworkInventory inventory, Completion completion) {
        completion.success();
    }

    void delete(L2PortGroupNetworkInventory inventory, Completion completion);
}
