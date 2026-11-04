package org.zstack.test.integration.networkservice.provider.flat.hostRoute

import org.zstack.header.network.service.NetworkServiceType
import org.zstack.sdk.L2NetworkInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.NetworkServiceProviderInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.flat.FlatNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.L3NetworkSpec
import org.zstack.testlib.SubCase

/**
 * Created by shixin on 04/13/2018.
 */
class FlatAddHostRouteAPICase extends SubCase {
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

    void testAddHostRouteApi() {
        L2NetworkInventory l2 = env.inventoryByName("l2")

        NetworkServiceProviderInventory networkServiceProvider = queryNetworkServiceProvider {
            delegate.conditions = ["type=Flat"]
        }[0]
        assert networkServiceProvider.networkServiceTypes.contains(NetworkServiceType.HostRoute.toString())

        L3NetworkInventory l3_1 = createL3Network {
            delegate.name = "l3-1"
            delegate.l2NetworkUuid = l2.uuid
            delegate.category = "Public"
        }

        /* public network will not attach host route */
        expect (AssertionError.class) {
            addHostRouteToL3Network {
                l3NetworkUuid = l3_1.uuid
                prefix = "10.1.1.0/24"
                nexthop = "1.1.1.1"
            }
        }

        L3NetworkInventory l3_2 = createL3Network {
            delegate.name = "l3-2"
            delegate.l2NetworkUuid = l2.uuid
            delegate.category = "Private"
        }

        Map<String, List<String>> netServices = ["${networkServiceProvider.uuid}":["HostRoute"]]
        attachNetworkServiceToL3Network {
            delegate.l3NetworkUuid = l3_2.uuid
            delegate.networkServices = netServices
        }
        addHostRouteToL3Network {
            l3NetworkUuid = l3_2.uuid
            prefix = "10.1.1.0/24"
            nexthop = "1.1.1.1"
        }

        L3NetworkInventory l3_3 = createL3Network {
            delegate.name = "l3-3"
            delegate.l2NetworkUuid = l2.uuid
            delegate.category = "Private"
        }

        NetworkServiceProviderInventory vRouter = queryNetworkServiceProvider {
            delegate.conditions = ["type=vrouter"]
        }[0]
        assert !vRouter.networkServiceTypes.contains(NetworkServiceType.HostRoute.toString())

        netServices = ["${vRouter.uuid}":["HostRoute"]]

        expect (AssertionError.class) {
            attachNetworkServiceToL3Network {
                delegate.l3NetworkUuid = l3_2.uuid
                delegate.networkServices = netServices
            }
        }
    }

    @Override
    void test() {
        env.create {
            l3 = (env.specByName("l3") as L3NetworkSpec).inventory
            testAddHostRouteApi()
        }
    }
}
