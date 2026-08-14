package org.zstack.test.integration.network.l3network.getfreeip

import org.zstack.sdk.FreeIpInventory
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.IpRangeSpec
import org.zstack.testlib.L3NetworkSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.network.NetworkUtils

/**
 * Created by xing5 on 2017/2/22.
 */
class OneL3TwoIpRangesCase extends SubCase {
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

                        ip {
                            name = "ipr"
                            startIp = "10.223.110.10"
                            endIp = "10.223.110.20"
                            gateway = "10.223.110.1"
                            netmask = "255.255.255.0"
                        }

                        ip {
                            name = "ipr2"
                            startIp = "10.223.110.50"
                            endIp = "10.223.110.60"
                            gateway = "10.223.110.1"
                            netmask = "255.255.255.0"
                        }
                    }
                }
            }
        }
    }

    void useL3NetworkUuidTestTwoIpRangesSize() {
        L3NetworkSpec l3 = env.specByName("l3")
        IpRangeSpec ipr1 = env.specByName("ipr")
        IpRangeSpec ipr2 = env.specByName("ipr2")

        List<FreeIpInventory> freeIps = getFreeIpOfL3Network {
            l3NetworkUuid = l3.inventory.uuid
        }

        assert freeIps.size() ==  NetworkUtils.ipRangeLength(ipr1.startIp, ipr1.endIp) + NetworkUtils.ipRangeLength(ipr2.startIp, ipr2.endIp)
    }

    void useTwoIpRangesUuidTestTwoIpRangesSize() {
        IpRangeSpec ipr1 = env.specByName("ipr")
        List<FreeIpInventory> freeIps = getFreeIpOfIpRange {
            ipRangeUuid = ipr1.inventory.uuid
        }

        assert freeIps.size() == NetworkUtils.ipRangeLength(ipr1.startIp, ipr1.endIp)

        IpRangeSpec ipr2 = env.specByName("ipr2")
        freeIps = getFreeIpOfIpRange {
            ipRangeUuid = ipr2.inventory.uuid
        }

        assert freeIps.size() == NetworkUtils.ipRangeLength(ipr2.startIp, ipr2.endIp)
    }

    @Override
    void test() {
        env.create {
            useL3NetworkUuidTestTwoIpRangesSize()
            useTwoIpRangesUuidTestTwoIpRangesSize()
            useTwoIpRangesUuidTestTwoIpRangesSize()
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
