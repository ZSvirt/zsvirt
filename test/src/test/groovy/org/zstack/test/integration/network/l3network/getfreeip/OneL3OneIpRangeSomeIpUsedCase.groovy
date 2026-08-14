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
class OneL3OneIpRangeSomeIpUsedCase extends SubCase {
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
                    }
                }
            }
        }
    }


    void occupyFourIPs() {
        L3NetworkSpec l3 = env.specByName("l3")

        for (int i=0; i<4; i++) {
            createVip {
                name = "non-use-vip"
                l3NetworkUuid = l3.inventory.uuid
            }
        }
    }

    void useL3NetworkUuidGetFreeIPsAfterOccupyFourIPs() {
        L3NetworkSpec l3 = env.specByName("l3")
        IpRangeSpec ipr = env.specByName("ipr")

        List<FreeIpInventory> freeIps = getFreeIpOfL3Network {
            l3NetworkUuid = l3.inventory.uuid
        }

        assert freeIps.size() == NetworkUtils.ipRangeLength(ipr.inventory.startIp, ipr.inventory.endIp) - 4
    }

    void useIpRangeUuidGetFreeIPsAfterOccupyFourIPs() {
        IpRangeSpec ipr = env.specByName("ipr")

        List<FreeIpInventory> freeIps = getFreeIpOfIpRange {
            ipRangeUuid = ipr.inventory.uuid
        }

        assert freeIps.size() == NetworkUtils.ipRangeLength(ipr.inventory.startIp, ipr.inventory.endIp) - 4
    }

    def testGetFreeIpAction() {
        L3NetworkSpec l3 = env.specByName("l3")
        IpRangeSpec ipr = env.specByName("ipr")

        List<FreeIpInventory> freeIps = getFreeIp {
            l3NetworkUuid = l3.inventory.uuid
        }

        assert freeIps.size() == NetworkUtils.ipRangeLength(ipr.inventory.startIp, ipr.inventory.endIp) - 4

        freeIps = getFreeIp {
            ipRangeUuid = ipr.inventory.uuid
        }

        assert freeIps.size() == NetworkUtils.ipRangeLength(ipr.inventory.startIp, ipr.inventory.endIp) - 4
    }

    @Override
    void test() {
        env.create {
            occupyFourIPs()
            useL3NetworkUuidGetFreeIPsAfterOccupyFourIPs()
            useIpRangeUuidGetFreeIPsAfterOccupyFourIPs()
            testGetFreeIpAction()
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
