package org.zstack.test.integration.network.l3network

import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.L3NetworkInventory
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.sdk.NetworkServiceL3NetworkRefInventory

class AttachL3NetworkServicesCase extends SubCase {
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
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
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

            attachNetworkServiceToL3Network {
                l3NetworkUuid = l3.uuid
                networkServices = ['VirtualRouter':['DHCP','DNS']]
            }
	    l3 = queryL3Network { conditions = ["uuid=${l3.uuid}"]}[0]
	    assert !l3.networkServices.isEmpty()

            Map<String, List<String>> services = new HashMap<String, List<String>>()
            for (NetworkServiceL3NetworkRefInventory ref : l3.getNetworkServices()) {
                List<String> types = services.get(ref.getNetworkServiceProviderUuid())
                if (types == null) {
                    types = new ArrayList<String>()
                    services.put(ref.getNetworkServiceProviderUuid(), types)
                }
		types.add(ref.getNetworkServiceType())
            }

            detachNetworkServiceFromL3Network {
                l3NetworkUuid = l3.uuid
                networkServices = services
            }

            expect(AssertionError.class){
                attachNetworkServiceToL3Network{
                    l3NetworkUuid = l3.uuid
                    networkServices = ['SecurityGroup':[]]
                }
            }

            expect(AssertionError.class){
                attachNetworkServiceToL3Network{
                    l3NetworkUuid = l3.uuid
                    networkServices = ['ErrorType':['DHCP','DNS']]
                }
            }
        }
    }

}

