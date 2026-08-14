package org.zstack.test.integration.networkservice.provider.flat.hostRoute

import org.zstack.header.network.service.NetworkServiceConstants
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.sdk.*
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.flat.FlatNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.L3NetworkSpec
import org.zstack.testlib.SubCase

/**
 * Created by shixin on 04/10/2018.
 */
class FlatAddHostRouteForMetaDataCase extends SubCase {
    EnvSpec env

    L3NetworkInventory l3

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
        env = FlatNetworkServiceEnv.oneHostNoVmEnv()
    }

    void testMetaDataHostRoute() {
        l3 = (env.specByName("l3") as L3NetworkSpec).inventory
        HostInventory host1 = env.inventoryByName("kvm")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        ImageInventory image = env.inventoryByName("image") as ImageInventory

        createVmInstance {
            name = "test-1"
            l3NetworkUuids = [l3.uuid]
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            hostUuid = host1.uuid
        }

        l3 = queryL3Network { delegate.conditions = ["name=l3"] }[0]
        GetL3NetworkDhcpIpAddressResult dhcpIp = getL3NetworkDhcpIpAddress {
            l3NetworkUuid = l3.uuid
        }

        assert l3.hostRoute.size() == 1
        for (L3NetworkHostRouteInventory route: l3.hostRoute) {
            assert route.prefix == NetworkServiceConstants.METADATA_HOST_PREFIX
            assert route.nexthop == dhcpIp.ip
        }
    }

    @Override
    void test() {
        env.create {
            testMetaDataHostRoute()
        }
    }
}
