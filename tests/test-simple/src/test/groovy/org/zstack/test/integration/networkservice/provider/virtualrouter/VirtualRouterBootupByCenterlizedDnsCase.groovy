package org.zstack.test.integration.networkservice.provider.virtualrouter

import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.NetworkServiceProviderInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
/**
 * Created by shixin on 06/07/18.
 */
class VirtualRouterBootupByCenterlizedDnsCase extends SubCase {
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
        env = VirtualRouterNetworkServiceEnv.ForHostsVyosOnEipEnv()
    }

    @Override
    void test() {
        env.create {
            testCreateMultipleVmWithVrouterNetwork()
        }
    }

    void testCreateMultipleVmWithVrouterNetwork() {
        def l3nw = env.inventoryByName("l3-1") as L3NetworkInventory
        def image = env.inventoryByName("image") as ImageInventory
        def offer = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        
        createVmInstance {
            name = "vm-test"
            instanceOfferingUuid = offer.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3nw.uuid]
        }
    }
}
