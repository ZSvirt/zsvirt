package org.zstack.test.integration.networkservice.provider.virtualrouter.eip

import org.zstack.sdk.EipInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VirtualRouterVmInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.virtualrouter.VirtualRouterNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by weiwang on 21/11/2017
 */
class VirtualRouterVipsCase extends SubCase {
    EnvSpec env

    VmInstanceInventory vm
    EipInventory eip
    EipInventory eip1

    @Override
    void clean() {
        env.cleanSimulatorHandlers();
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = VirtualRouterNetworkServiceEnv.oneVmOneHostVyosOnEipEnv()
    }

    @Override
    void test() {
        env.create {
            vm = env.inventoryByName("vm")
            eip = env.inventoryByName("eip")
            eip1 = env.inventoryByName("eip1")

            testGetVirtualRouterVm()
        }
    }

    void testGetVirtualRouterVm() {
        VirtualRouterVmInventory vrvmInv = queryVirtualRouterVm {}[0]
        assert vrvmInv.virtualRouterVips.size() == 1

        attachEip {
            delegate.eipUuid = eip.uuid
            delegate.vmNicUuid = vm.vmNics[0].uuid
        }
        vrvmInv = queryVirtualRouterVm {}[0]
        assert vrvmInv.virtualRouterVips.size() == 2

        stopVmInstance {
            delegate.uuid = vm.uuid
        }
        vrvmInv = queryVirtualRouterVm {}[0]
        assert vrvmInv.virtualRouterVips.size() == 1

        attachEip {
            delegate.eipUuid = eip1.uuid
            delegate.vmNicUuid = vm.vmNics[0].uuid
        }
        vrvmInv = queryVirtualRouterVm {}[0]
        assert vrvmInv.virtualRouterVips.size() == 1

        startVmInstance {
            delegate.uuid = vm.uuid
        }
        vrvmInv = queryVirtualRouterVm {}[0]
        assert vrvmInv.virtualRouterVips.size() == 3
    }

}
