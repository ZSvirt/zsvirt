package org.zstack.test.integration.network.l3network

import org.zstack.core.cloudbus.CloudBus
import org.zstack.header.network.l3.AllocateIpMsg
import org.zstack.header.network.l3.AllocateIpReply
import org.zstack.header.network.l3.L3NetworkConstant
import org.zstack.network.service.flat.FlatNetworkSystemTags
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.L3NetworkSpec
import org.zstack.testlib.SubCase

class DhcpIpStatisticCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
            zone {
                name = "zone"
                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"
                    l3Network {
                        name = "l3"
                        ip {
                            name = "ipr"
                            startIp = "10.0.0.10"
                            endIp = "10.0.0.100"
                            gateway = "10.0.0.1"
                            netmask = "255.255.255.0"
                        }
                    }
                }
            }
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void test() {
        env.create {
            testDhcpFilteringWithDatabase()
        }
    }

    void testDhcpFilteringWithDatabase() {
        L3NetworkSpec l3 = env.specByName("l3")
        String l3Uuid = l3.inventory.uuid
        CloudBus bus = bean(CloudBus)
        def usedIps = ["10.0.0.10", "10.0.0.20", "10.0.0.30"].collect { address ->
            AllocateIpMsg msg = new AllocateIpMsg(l3NetworkUuid: l3Uuid, requiredIp: address)
            bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, l3Uuid)
            def reply = bus.call(msg)
            assert reply.success : reply.error
            return (reply as AllocateIpReply).ipInventory
        }

        def noDhcp = queryDhcp(l3Uuid)
        assert noDhcp.ipStatistics.empty
        assert noDhcp.total == 0L

        usedIps.tail().each { usedIp ->
            def creator = FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.newSystemTagCreator(l3Uuid)
            creator.inherent = true
            creator.setTagByTokens([
                    (FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_TOKEN): usedIp.ip,
                    (FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_UUID_TOKEN): usedIp.uuid])
            creator.create()
        }

        def all = getL3NetworkIpStatistic {
            l3NetworkUuid = l3Uuid
            resourceType = "All"
            sortBy = "Ip"
            sortDirection = "asc"
            replyWithCount = true
        }
        assert all.ipStatistics*.usedIpUuid == usedIps*.uuid
        assert all.ipStatistics*.resourceTypes == [["Other"], ["DHCP"], ["DHCP"]]
        assert all.total == 3L

        def dhcp = queryDhcp(l3Uuid)
        assert dhcp.ipStatistics*.usedIpUuid == usedIps.tail()*.uuid
        assert dhcp.ipStatistics*.resourceTypes == [["DHCP"], ["DHCP"]]
        assert dhcp.total == 2L

        def page = queryDhcp(l3Uuid, 1, 1)
        assert page.ipStatistics*.usedIpUuid == [usedIps[2].uuid]
        assert page.ipStatistics*.ip == ["10.0.0.30"]
        assert page.total == 2L

        def filtered = queryDhcp(l3Uuid, 0, 1, "10.0.0.3%")
        assert filtered.ipStatistics*.usedIpUuid == [usedIps[2].uuid]
        assert filtered.total == 1L

        def pastEnd = queryDhcp(l3Uuid, 1, 1, "10.0.0.3%")
        assert pastEnd.ipStatistics.empty
        assert pastEnd.total == 1L

        def nonDhcp = queryDhcp(l3Uuid, 0, 1, "10.0.0.10")
        assert nonDhcp.ipStatistics.empty
        assert nonDhcp.total == 0L
    }

    private def queryDhcp(String l3Uuid, int offset = 0, int pageSize = 20, String ipFilter = null) {
        return getL3NetworkIpStatistic {
            l3NetworkUuid = l3Uuid
            resourceType = "DHCP"
            sortBy = "Ip"
            sortDirection = "asc"
            start = offset
            limit = pageSize
            ip = ipFilter
            replyWithCount = true
        }
    }
}
