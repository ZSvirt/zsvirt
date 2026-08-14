package org.zstack.test.integration.network.l3network.getfreeip

import org.zstack.sdk.FreeIpInventory
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.IpRangeSpec
import org.zstack.testlib.L3NetworkSpec
import org.zstack.sdk.*
import org.zstack.testlib.SubCase
import org.zstack.utils.network.NetworkUtils
import org.zstack.utils.network.IPv6Constants


/**
 * Created by anquan on 2020/11/24.
 */
class OneL3DualStackIpRangesCase extends SubCase {
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
                        name = "l3"
                    }
                }
            }
        }
    }


    @Override
    void test() {
        env.create {

            def l3 = env.inventoryByName("l3") as L3NetworkInventory

            IpRangeInventory ipr4 = addIpRange {
                name = "ipr-4"
                l3NetworkUuid = l3.uuid
                startIp = "10.223.110.10"
                endIp = "10.223.110.20"
                gateway = "10.223.110.1"
                netmask = "255.255.255.0"
            }

            IpRangeInventory ipr6 = addIpv6Range {
                name = "ipr-6"
                l3NetworkUuid = l3.uuid
                startIp = "2003:2001::0010"
                endIp = "2003:2001::0020"
                gateway = "2003:2001::2"
                prefixLen = 64
                addressMode = IPv6Constants.Stateful_DHCP
            }

            List<FreeIpInventory> freeIps = getFreeIpOfIpRange {
                ipRangeUuid = ipr6.uuid
            }
            assert freeIps.size() ==  17

            freeIps = getFreeIpOfL3Network {
                l3NetworkUuid = l3.uuid
            }

            assert freeIps.size() ==  28


        }
    }


    @Override
    void clean() {
        env.delete()
    }
}
