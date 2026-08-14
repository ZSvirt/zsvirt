package org.zstack.test.integration.networkservice.provider.virtualrouter.dns

import org.springframework.http.HttpEntity
import org.zstack.network.service.virtualrouter.VirtualRouterCommands
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.L3NetworkInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.virtualrouter.VirtualRouterNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.L3NetworkSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by weiwang on 25/05/2017.
 */
class VirtualRouterAddDnsCase extends SubCase {
    EnvSpec env

    L3NetworkInventory l3
    String dns1 = "8.8.8.8"
    String dns2 = "8.8.4.4"
    String dns3 = "8.8.4.5"
    String dns4 = "8.8.4.6"
    String dns5 = "1.2.255.666"
    String dns6 = "1:2:1:1::::::::::a"

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
        env = VirtualRouterNetworkServiceEnv.oneVmOneHostVyosOnEipEnv()
    }

    void testAddDns() {
        VirtualRouterCommands.SetDnsCmd cmd = null

        env.afterSimulator(VirtualRouterConstant.VR_SET_DNS_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, VirtualRouterCommands.SetDnsCmd.class)
            return rsp
        }

        addDnsToL3Network {
            delegate.l3NetworkUuid = l3.getUuid()
            delegate.dns = dns1
        }

        expect(AssertionError.class) {
            addDnsToL3Network {
                delegate.l3NetworkUuid = l3.getUuid()
                delegate.dns = dns5
            }
        }

        expect(AssertionError.class)  {
            addDnsToL3Network {
                delegate.l3NetworkUuid = l3.getUuid()
                delegate.dns = dns6
            }
        }

        assert cmd != null
        assert cmd.dns.stream()
                      .map { dnsinfo -> dnsinfo.dnsAddress }
                      .collect()
                      .contains(dns1)

        addDnsToL3Network {
            delegate.l3NetworkUuid = l3.getUuid()
            delegate.dns = dns2
        }
        cmd == null
        addDnsToL3Network {
            delegate.l3NetworkUuid = l3.getUuid()
            delegate.dns = dns3
        }

        assert cmd != null
        assert cmd.dns.size() == 3
        assert cmd.dns.get(0).dnsAddress == dns1
        assert cmd.dns.get(1).dnsAddress == dns2
        assert cmd.dns.get(2).dnsAddress == dns3

        cmd = null
        removeDnsFromL3Network {
            l3NetworkUuid = l3.getUuid()
            dns = dns1
        }
        assert cmd != null
        assert cmd.dns.size() == 2
        assert cmd.dns.get(0).dnsAddress == dns2
        assert cmd.dns.get(1).dnsAddress == dns3

        cmd = null
        addDnsToL3Network {
            delegate.l3NetworkUuid = l3.getUuid()
            delegate.dns = dns4
        }
        assert cmd != null
        assert cmd.dns.size() == 3
        assert cmd.dns.get(0).dnsAddress == dns2
        assert cmd.dns.get(1).dnsAddress == dns3
        assert cmd.dns.get(2).dnsAddress == dns4
    }

    @Override
    void test() {
        env.create {
            l3 = (env.specByName("l3") as L3NetworkSpec).inventory
            testAddDns()
        }
    }
}
