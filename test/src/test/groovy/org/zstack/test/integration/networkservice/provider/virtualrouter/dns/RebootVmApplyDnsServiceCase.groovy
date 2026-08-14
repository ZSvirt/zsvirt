package org.zstack.test.integration.networkservice.provider.virtualrouter.dns

import org.springframework.http.HttpEntity
import org.zstack.network.service.virtualrouter.VirtualRouterCommands
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.virtualrouter.VirtualRouterNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.L3NetworkSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by shixin on 01/12/2018.
 */
class RebootVmApplyDnsServiceCase extends SubCase {
    EnvSpec env

    L3NetworkInventory l3
    String dns1 = "8.8.8.8"
    String dns2 = "8.8.4.4"
    String dns3 = "8.8.4.5"
    String dns4 = "8.8.4.6"

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
        VmInstanceInventory vm = env.inventoryByName("vm")
        L3NetworkInventory pubL3 = env.inventoryByName("pubL3")

        addDnsToL3Network {
            delegate.l3NetworkUuid = l3.getUuid()
            delegate.dns = dns1
        }
        addDnsToL3Network {
            delegate.l3NetworkUuid = l3.getUuid()
            delegate.dns = dns2
        }
        addDnsToL3Network {
            delegate.l3NetworkUuid = l3.getUuid()
            delegate.dns = dns3
        }
        /* public network dns will not be included in setdns */
        addDnsToL3Network {
            delegate.l3NetworkUuid = pubL3.getUuid()
            delegate.dns = dns4
        }

        env.afterSimulator(VirtualRouterConstant.VR_SET_DNS_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, VirtualRouterCommands.SetDnsCmd.class)
            return rsp
        }

        rebootVmInstance {
            uuid = vm.uuid
        }

        assert cmd != null
        assert cmd.dns.size() == 3
        assert cmd.dns.get(0).dnsAddress == dns1
        assert cmd.dns.get(1).dnsAddress == dns2
        assert cmd.dns.get(2).dnsAddress == dns3
    }

    @Override
    void test() {
        env.create {
            l3 = (env.specByName("l3") as L3NetworkSpec).inventory
           // testAddDns()
        }
    }
}
