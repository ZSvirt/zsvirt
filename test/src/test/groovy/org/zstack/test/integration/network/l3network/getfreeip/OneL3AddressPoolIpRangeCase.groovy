package org.zstack.test.integration.network.l3network.getfreeip

import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.sdk.FreeIpInventory
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.IpRangeSpec
import org.zstack.testlib.L3NetworkSpec
import org.zstack.sdk.*
import org.zstack.testlib.SubCase
import org.zstack.utils.network.NetworkUtils
import org.zstack.utils.network.IPv6Constants

class OneL3AddressPoolIpRangeCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
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
                        category = "Public"
                        name = "l3"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), NetworkServiceType.DNS.toString()]
                        }   
                    }
                }
            }
        }
    }


    @Override
    void test() {
        env.create {

            def l3 = env.inventoryByName("l3") as L3NetworkInventory

            IpRangeInventory ipr4_pool =  addIpRange {
                name = "address-pool"
                l3NetworkUuid = l3.uuid
                startIp = "100.64.101.101"
                endIp = "100.64.101.101"
                netmask = "255.255.255.0"
                ipRangeType = IpRangeType.AddressPool.toString()
            }

            // check getFreeIp when 'gateway' in AddressPool
            List<FreeIpInventory> freeIps = getFreeIpOfIpRange {
                ipRangeUuid = ipr4_pool.uuid
            }
            assert freeIps.size() == 1

            freeIps = getFreeIpOfL3Network {
                l3NetworkUuid = l3.uuid
            }
            assert freeIps.size() ==  1

            // check createEip when 'gateway' in AddressPool
            VipInventory vip1 = createVip {
                name = "address-pool-eip"
                l3NetworkUuid = l3.uuid
                ipRangeUuid = ipr4_pool.uuid
                requiredIp = "100.64.101.101"
            }
            EipInventory eip1 = createEip {
                name = "eip-1"
                vipUuid = vip1.uuid
            }
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
