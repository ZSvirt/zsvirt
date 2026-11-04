package org.zstack.test.integration.network.l3network.ipv6

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmSystemTags
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.*
import org.zstack.test.integration.network.l3network.Env
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.util.stream.Collectors

import static java.util.Arrays.asList

/**
 * Created by shixin on 2018/11/26.
 */
class IPv6SpoofingCase extends SubCase {
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
            testIPv6Spoofing()
        }
    }

    void testIPv6Spoofing() {
        L3NetworkInventory l3_statefull = env.inventoryByName("l3-Statefull-DHCP")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")

        KVMAgentCommands.StartVmCmd startCmd = new KVMAgentCommands.StartVmCmd()
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> entity ->
            startCmd = json(entity.getBody(), KVMAgentCommands.StartVmCmd)
            return rsp
        }
        addIpRangeByNetworkCidr {
            name = "ipr4-1"
            l3NetworkUuid = l3_statefull.getUuid()
            networkCidr = "192.168.110.0/24"
        }

        VmInstanceInventory vm = createVmInstance {
            name = "vm-spoofing"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = asList(l3_statefull.uuid)
            systemTags = [String.format("%s::%s", VmSystemTags.CLEAN_TRAFFIC_TOKEN, Boolean.TRUE.toString())]
        }
        VmNicInventory nic = vm.getVmNics()[0]
        assert startCmd.nics.size() == 1
        KVMAgentCommands.NicTO nicTO = startCmd.nics.get(0)
        assert nicTO.ips.size() == 2
    }

}

