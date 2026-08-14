package org.zstack.test.integration.networkservice.provider.flat.dns

import org.springframework.http.HttpEntity
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.sdk.IpRangeInventory
import org.zstack.sdk.L2NetworkInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.flat.FlatNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.L3NetworkSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.network.IPv6Constants

/**
 * Created by weiwang on 25/05/2017.
 */
class FlatAddDnsCase extends SubCase {
    EnvSpec env

    L3NetworkInventory l3
    String dns1 = "8.8.8.8"

    @Override
    void clean() {
        env.cleanSimulatorHandlers()
        env.delete()
    }


    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = FlatNetworkServiceEnv.oneFlatEipEnv()
    }

    void testAddDns() {
        FlatDhcpBackend.BatchApplyDhcpCmd cmd = null
        env.afterSimulator(FlatDhcpBackend.BATCH_APPLY_DHCP_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, FlatDhcpBackend.BatchApplyDhcpCmd.class)
            return rsp
        }

        addDnsToL3Network {
            delegate.l3NetworkUuid = l3.getUuid()
            delegate.dns = dns1
        }

        assert cmd != null
        assert cmd.dhcpInfos.get(0).dhcp.get(0).dns.size() == 1
        assert cmd.dhcpInfos.get(0).dhcp.get(0).dns.get(0) == dns1

        def l2 = env.inventoryByName("l2") as L2NetworkInventory

        L3NetworkInventory l3_pub_ipv6 = createL3Network {
            category = "Public"
            l2NetworkUuid = l2.uuid
            name = "ipv6"
            ipVersion = 6
        }

        IpRangeInventory ipr1 = addIpv6Range {
            l3NetworkUuid = l3_pub_ipv6.uuid
            name = "test-ipv6-range"
            startIp = "1000::10"
            endIp = "1000::20"
            prefixLen = 64
            gateway = "1000::1"
            addressMode = IPv6Constants.Stateful_DHCP
            systemTags = [String.format("flatNetwork::DhcpServer::%s::ipUuid::null", "1000--2")]
        }

        addDnsToL3Network {
            delegate.l3NetworkUuid = l3_pub_ipv6.getUuid()
            delegate.dns = "1::1"
        }

        expectError {
            addDnsToL3Network {
                delegate.l3NetworkUuid = l3_pub_ipv6.getUuid()
                delegate.dns = "1::0:1"
            }
        }
    }

    @Override
    void test() {
        env.create {
            l3 = (env.specByName("l3") as L3NetworkSpec).inventory
            testAddDns()
        }
    }
}
