package org.zstack.test.integration.network.l3network.ipv6

import org.springframework.http.HttpEntity
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.sdk.*
import org.zstack.test.integration.network.l3network.Env
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.network.IPv6Constants

import static java.util.Arrays.asList

/**
 * Created by shixin on 2018/10/15.
 */
class IPv6MigrateVmCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }
    @Override
    void environment() {
        env = Env.Ipv6FlatL3Network()
    }

    @Override
    void test() {
        env.create {
            testMirateVm()
        }
    }

    void testMirateVm() {
        L3NetworkInventory l3_statefull = env.inventoryByName("l3-Statefull-DHCP")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")
        HostInventory h1 = env.inventoryByName("kvm-1")
        HostInventory h2 = env.inventoryByName("kvm-2")

        addIpRangeByNetworkCidr {
            name = "ipr4-1"
            l3NetworkUuid = l3_statefull.getUuid()
            networkCidr = "192.168.110.0/24"
        }
        VmInstanceInventory vm = createVmInstance {
            name = "vm-eip"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = asList(l3_statefull.uuid)
            hostUuid = h1.uuid
        }

        List<FlatDhcpBackend.BatchApplyDhcpCmd> cmds = new ArrayList()
        env.afterSimulator(FlatDhcpBackend.BATCH_APPLY_DHCP_PATH) { rsp, HttpEntity<String> e1 ->
            FlatDhcpBackend.BatchApplyDhcpCmd cmd = JSONObjectUtil.toObject(e1.body, FlatDhcpBackend.BatchApplyDhcpCmd.class)
            cmds.add(cmd)
            return rsp
        }

        migrateVm {
            vmInstanceUuid = vm.uuid
            hostUuid = h2.uuid
        }

        assert cmds.size() == 1

        reconnectHost {
            uuid = h2.uuid
        }
    }

}

