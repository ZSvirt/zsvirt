package org.zstack.test.integration.networkservice.provider.flat.eip

import org.zstack.header.network.l3.L3NetworkConstant
import org.zstack.network.service.eip.EipConstant
import org.zstack.sdk.EipInventory
import org.zstack.sdk.FreeIpInventory
import org.zstack.sdk.L2NetworkInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.NetworkServiceProviderInventory
import org.zstack.sdk.VipInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.flat.FlatNetworkServiceEnv
import org.zstack.testlib.*
import org.zstack.utils.network.IPv6Constants

/**
 * Created by shixin.ruan on 2024/09/02.
 */
class NoDhcpNetworkEipCase extends SubCase {
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
        env = FlatNetworkServiceEnv.oneFlatEipEnv()
    }


    @Override
    void test() {
        env.create {
            testAttachEipToNicWithoutGateway()
        }
    }

    void testAttachEipToNicWithoutGateway() {
        def vm = env.inventoryByName("vm") as VmInstanceInventory
        def pubL3 = env.inventoryByName("pubL3") as L3NetworkInventory
        def l2 = env.inventoryByName("l2") as L2NetworkInventory

        def noIPAML3 = createL3Network {
            name = "l3-no-dhcp"
            l2NetworkUuid = l2.uuid
            type = L3NetworkConstant.L3_BASIC_NETWORK_TYPE.toString()
            category = "Private"
            enableIPAM = false
        } as L3NetworkInventory

        def nsProviders = queryNetworkServiceProvider {
            delegate.conditions = ["type=Flat"]
        } as List<NetworkServiceProviderInventory>
        Map<String, List<String>> netServices = new HashMap<>()
        netServices.put(nsProviders.get(0).uuid,
                [EipConstant.EIP_NETWORK_SERVICE_TYPE])

        attachNetworkServiceToL3Network {
            l3NetworkUuid = noIPAML3.uuid
            networkServices = netServices
        }

        def nic1 = vm.vmNics.get(0) as VmNicInventory
        detachL3NetworkFromVm {
            vmNicUuid = nic1.uuid
        }

        /* disable dhcp will not allocate ip address to vmnic */
        def freeIp4s = getFreeIp {
            l3NetworkUuid = pubL3.getUuid()
            ipVersion = IPv6Constants.IPv4
            limit = 1
        } as List<FreeIpInventory>
        String ip = freeIp4s.get(0).getIp()
        String netmask = freeIp4s.get(0).getNetmask()

        attachL3NetworkToVm {
            l3NetworkUuid = noIPAML3.uuid
            vmInstanceUuid = vm.uuid
            systemTags = [String.format("staticIp::%s::%s", noIPAML3.uuid, ip),
                          String.format("ipv4Netmask::%s::%s", noIPAML3.uuid, netmask)]
        }

        vm = (queryVmInstance { conditions = ["uuid=${vm.uuid}"] } as List<VmInstanceInventory>).get(0)
        nic1 = (vm.vmNics as List<VmNicInventory>).get(0)
        def vip = createVip {
            name = "vip1"
            l3NetworkUuid = pubL3.uuid
        } as VipInventory

        expect(AssertionError.class) {
            createEip {
                name = "eip4"
                vipUuid = vip.uuid
                vmNicUuid = nic1.uuid
            }
        }

        def eip = createEip {
            name = "eip4"
            vipUuid = vip.uuid
        } as EipInventory

        expect(AssertionError.class) {
            attachEip {
                eipUuid = eip.uuid
                vmNicUuid = nic1.uuid
            }
        }
    }
}
