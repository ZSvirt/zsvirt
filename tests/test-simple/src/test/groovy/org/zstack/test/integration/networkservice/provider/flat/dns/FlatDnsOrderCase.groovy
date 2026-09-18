package org.zstack.test.integration.networkservice.provider.flat.dns

import org.springframework.http.HttpEntity
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.network.service.flat.FlatNetwordProviderGlobalConfig
import org.zstack.sdk.GetL3NetworkDhcpIpAddressResult
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.flat.FlatNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.L3NetworkSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

class FlatDnsOrderCase extends SubCase {
    EnvSpec env

    VmInstanceInventory vm
    L3NetworkInventory l3

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = FlatNetworkServiceEnv.oneFlatEipEnv()
    }

    @Override
    void test() {
        env.create {
            l3 = (env.specByName("l3") as L3NetworkSpec).inventory
            testDnsOder()
        }
    }

    void testDnsOder() {
        updateGlobalConfig {
            category = FlatNetwordProviderGlobalConfig.CATRGORY
            name = "allow.default.dns"
            value = true
        }

        VmInstanceInventory vm = env.inventoryByName("vm")

        def ret = getL3NetworkDhcpIpAddress {
            l3NetworkUuid = l3.getUuid()
        } as GetL3NetworkDhcpIpAddressResult

        addDnsToL3Network {
            l3NetworkUuid = l3.uuid
            dns = "1.1.1.1"
        }

        addDnsToL3Network {
            l3NetworkUuid = l3.uuid
            dns = "1.1.1.2"
        }

        L3NetworkInventory l3 = addDnsToL3Network {
            l3NetworkUuid = l3.uuid
            dns = "1.1.1.3"
        }
        assert l3.dns.get(0) == '1.1.1.1'
        assert l3.dns.get(1) == '1.1.1.2'
        assert l3.dns.get(2) == '1.1.1.3'

        FlatDhcpBackend.ApplyDhcpCmd cmd = null
        env.simulator(FlatDhcpBackend.BATCH_APPLY_DHCP_PATH){HttpEntity<String> e,EnvSpec spec ->
            FlatDhcpBackend.BatchApplyDhcpCmd batchApplyDhcpCmd = JSONObjectUtil.toObject(e.body, FlatDhcpBackend.BatchApplyDhcpCmd.class)
            cmd = batchApplyDhcpCmd.dhcpInfos.get(0)
            def rsp = new FlatDhcpBackend.ApplyDhcpRsp()
            rsp.success = true
            return rsp
        }
        rebootVmInstance {
            uuid = vm.uuid
        }

        assert cmd != null
        assert cmd.dhcp.size() == 1
        assert cmd.dhcp.get(0).dns.size() == 4
        assert cmd.dhcp.get(0).dns.get(0) == "1.1.1.1"
        assert cmd.dhcp.get(0).dns.get(1) == "1.1.1.2"
        assert cmd.dhcp.get(0).dns.get(2) == "1.1.1.3"
        assert cmd.dhcp.get(0).dns.get(3) == ret.ip

        l3 = removeDnsFromL3Network {
            l3NetworkUuid = l3.uuid
            dns = "1.1.1.2"
        }
        assert l3.dns.get(0) == '1.1.1.1'
        assert l3.dns.get(1) == '1.1.1.3'

        cmd = null
        rebootVmInstance {
            uuid = vm.uuid
        }

        assert cmd != null
        assert cmd.dhcp.size() == 1
        assert cmd.dhcp.get(0).dns.size() == 3
        assert cmd.dhcp.get(0).dns.get(0) == "1.1.1.1"
        assert cmd.dhcp.get(0).dns.get(1) == "1.1.1.3"
        assert cmd.dhcp.get(0).dns.get(2) == ret.ip

        addDnsToL3Network {
            l3NetworkUuid = l3.uuid
            dns = "1.1.1.4"
        }
        l3 = addDnsToL3Network {
            l3NetworkUuid = l3.uuid
            dns = "1.1.1.2"
        }
        assert l3.dns.get(0) == '1.1.1.1'
        assert l3.dns.get(1) == '1.1.1.3'
        assert l3.dns.get(2) == '1.1.1.4'
        assert l3.dns.get(3) == '1.1.1.2'

        cmd = null
        rebootVmInstance {
            uuid = vm.uuid
        }

        assert cmd != null
        assert cmd.dhcp.size() == 1
        assert cmd.dhcp.get(0).dns.size() == 5
        assert cmd.dhcp.get(0).dns.get(0) == "1.1.1.1"
        assert cmd.dhcp.get(0).dns.get(1) == "1.1.1.3"
        assert cmd.dhcp.get(0).dns.get(2) == "1.1.1.4"
        assert cmd.dhcp.get(0).dns.get(3) == "1.1.1.2"
        assert cmd.dhcp.get(0).dns.get(4) == ret.ip
    }

    @Override
    void clean() {
        env.delete()
    }
}
