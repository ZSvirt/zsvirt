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
class OneL3OneIpRangeNoIpUsedCase extends SubCase {
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

    @Override
    void test() {
        env.create {
            useL3NetworkUuidWithStartIpAndLimit()
            useL3NetworkUuidWithStartBeyondTheEndIp()
            useL3NetworkUuidWithStartEqualsToIpJustBeforeTheEndIp()
            useL3NetworkUuidWithStartEqualsToTheEndIp()
            useL3NetworkUuidWithLimitLessThanEnoughIp()

            useIpRangeUuidWithStartIpAndLimit()
            useIpRangeUuidWithStartBeyondTheEndIp()
            useIpRangeUuidWithStartEqualsToIpJustBeforeTheEndIp()
            useIpRangeUuidWithStartEqualsToTheEndIp()
            useIpRangeUuidWithLimitLessThanEnoughIp()
        }
    }


    void useIpRangeUuidWithStartBeyondTheEndIp() {
        IpRangeSpec ipr = env.specByName("ipr")

        List<FreeIpInventory> freeIps = getFreeIpOfIpRange {
            ipRangeUuid = ipr.inventory.uuid
            start = "10.223.110.21"
        }

        assert freeIps.size() == 0
    }

    void useIpRangeUuidWithStartEqualsToTheEndIp() {
        IpRangeSpec ipr = env.specByName("ipr")
        List<FreeIpInventory> freeIps = getFreeIpOfIpRange {
            ipRangeUuid = ipr.inventory.uuid
            start = "10.223.110.20"
        }

        assert freeIps.size() == 1
        def ip1 = freeIps[0]
        assert ip1.ip == "10.223.110.20"
    }


    void useL3NetworkUuidWithStartBeyondTheEndIp() {
        L3NetworkSpec l3 = env.specByName("l3")
        List<FreeIpInventory> freeIps = getFreeIpOfL3Network {
            l3NetworkUuid = l3.inventory.uuid
            start = "10.223.110.21"
        }

        assert freeIps.size() == 0
    }

    void useL3NetworkUuidWithStartEqualsToTheEndIp() {
        L3NetworkSpec l3 = env.specByName("l3")
        List<FreeIpInventory> freeIps = getFreeIpOfL3Network {
            l3NetworkUuid = l3.inventory.uuid
            start = "10.223.110.20"
        }

        assert freeIps.size() == 1
        def ip1 = freeIps[0]
        assert ip1.ip == "10.223.110.20"
    }

    void useL3NetworkUuidWithStartEqualsToIpJustBeforeTheEndIp() {
        L3NetworkSpec l3 = env.specByName("l3")
        List<FreeIpInventory> freeIps = getFreeIpOfL3Network {
            l3NetworkUuid = l3.inventory.uuid
            start = "10.223.110.19"
        }

        assert freeIps.size() == 2
    }

    void useIpRangeUuidWithStartEqualsToIpJustBeforeTheEndIp() {
        IpRangeSpec ipr = env.specByName("ipr")
        List<FreeIpInventory> freeIps = getFreeIpOfIpRange {
            ipRangeUuid = ipr.inventory.uuid
            start = "10.223.110.19"
        }

        assert freeIps.size() == 2
    }

    void useIpRangeUuidWithLimitLessThanEnoughIp() {
        IpRangeSpec ipr = env.specByName("ipr")
        List<FreeIpInventory> freeIps = getFreeIpOfIpRange {
            ipRangeUuid = ipr.inventory.uuid
            start = "10.223.110.9"
            limit = 4
        }

        assert freeIps.size() == 4
    }

    void useL3NetworkUuidWithLimitLessThanEnoughIp() {
        L3NetworkSpec l3 = env.specByName("l3")
        List<FreeIpInventory> freeIps = getFreeIpOfL3Network {
            l3NetworkUuid = l3.inventory.uuid
            start = "10.223.110.9"
            limit = 4
        }

        assert freeIps.size() == 4
    }

    void useL3NetworkUuidWithStartIpAndLimit() {
        L3NetworkSpec l3 = env.specByName("l3")
        IpRangeSpec ipr = env.specByName("ipr")

        List<FreeIpInventory> freeIps = getFreeIpOfL3Network {
            l3NetworkUuid = l3.inventory.uuid
            start = "10.223.110.10"
            limit = 11
        }

        assert freeIps.size() == 11

        def ip1 = freeIps[0]
        assert ip1.gateway == ipr.inventory.gateway
        assert ip1.netmask == ipr.inventory.netmask
        assert ip1.ipRangeUuid == ipr.inventory.uuid
        assert NetworkUtils.isIpv4InRange(ip1.ip, ipr.inventory.startIp, ipr.inventory.endIp)
    }

    void useIpRangeUuidWithStartIpAndLimit() {
        IpRangeSpec ipr = env.specByName("ipr")

        List<FreeIpInventory> freeIps = getFreeIpOfIpRange {
            ipRangeUuid = ipr.inventory.uuid
            start = "10.223.110.10"
            limit = 11
        }

        assert freeIps.size() == 11

        def ip1 = freeIps[0]
        assert ip1.gateway == ipr.inventory.gateway
        assert ip1.netmask == ipr.inventory.netmask
        assert ip1.ipRangeUuid == ipr.inventory.uuid
        assert NetworkUtils.isIpv4InRange(ip1.ip, ipr.inventory.startIp, ipr.inventory.endIp)
    }

    @Override
    void clean() {
        env.delete()
    }
}
