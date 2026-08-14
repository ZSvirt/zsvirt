package org.zstack.test.integration.networkservice.provider.virtualrouter

import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.IpRangeInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VirtualRouterVmInventory
import org.zstack.sdk.VmNicInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
/**
 * Created by shixin.ruan on 01/02/2019.
 */
class VirtualRouterStartCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        // This environment contains vr-offering but no VM.
        env = VirtualRouterNetworkServiceEnv.ForHostsVyosOnEipEnv()
    }

    @Override
    void test() {
        env.create {
            testStartVirtualRouter()
        }
    }

    void testStartVirtualRouter() {
        def l3nw = env.inventoryByName("l3") as L3NetworkInventory
        def image = env.inventoryByName("image") as ImageInventory
        def offer = env.inventoryByName("instanceOffering") as InstanceOfferingInventory

        createVmInstance {
            name = "test-vm"
            instanceOfferingUuid = offer.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3nw.uuid]
        }

        VirtualRouterVmInventory vr = queryVirtualRouterVm {}[0]

        stopVmInstance {
            uuid = vr.uuid
        }

        startVmInstance {
            uuid = vr.uuid
        }

        String gatewayIp = null
        vr = queryVirtualRouterVm {}[0]
        for (VmNicInventory nic : vr.getVmNics()) {
            if (nic.l3NetworkUuid == l3nw.uuid) {
                gatewayIp = nic.ip
            }
        }
        IpRangeInventory ipr = l3nw.ipRanges.get(0)
        assert gatewayIp == ipr.getGateway()
    }
}
