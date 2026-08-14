package org.zstack.test.integration.network.l3network

import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.L3NetworkInventory
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by xing5 on 2017/3/31.
 */
class DetachL3NetworkServicesCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
        spring {
            securityGroup()
        }
    }

    @Override
    void environment() {
        env = env {
            zone {
                name = "zone"

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        service {
                            provider = VirtualRouterConstant.VIRTUAL_ROUTER_PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(), NetworkServiceType.DNS.toString()]
                        }

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }
                    }

                    l3Network {
                        name = "l3-2"

                        service {
                            provider = VirtualRouterConstant.VIRTUAL_ROUTER_PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(), NetworkServiceType.DNS.toString()]
                        }

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }
                    }
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            testDetachNetworkServiceWithUuid()
            testDetachNetworkServiceWithType()
        }
    }

    void testDetachNetworkServiceWithUuid() {
        L3NetworkInventory l3 = env.inventoryByName("l3")

        detachNetworkServiceFromL3Network {
            l3NetworkUuid = l3.uuid
            networkServices = l3.networkServices.inject([:]) { map, col ->
                List lst = map[col.networkServiceProviderUuid]
                if (lst == null) {
                    lst = []
                    map[col.networkServiceProviderUuid] = lst
                }
                lst.add(col.networkServiceType)

                map
            }
        }

        l3 = queryL3Network { conditions = ["uuid=${l3.uuid}"]}[0]

        assert l3.networkServices.isEmpty()
    }

    void testDetachNetworkServiceWithType() {
        L3NetworkInventory l3 = env.inventoryByName("l3-2")

        detachNetworkServiceFromL3Network {
            l3NetworkUuid = l3.uuid
            networkServices = [
                    (VirtualRouterConstant.VIRTUAL_ROUTER_PROVIDER_TYPE):  [NetworkServiceType.DHCP.toString(), NetworkServiceType.DNS.toString()],
                    (SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE):  [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE],
            ]
        }

        l3 = queryL3Network { conditions = ["uuid=${l3.uuid}"]}[0]

        assert l3.networkServices.isEmpty()
    }
}
