package org.zstack.test.integration.networkservice.provider.virtualrouter.eip

import org.springframework.http.HttpEntity
import org.zstack.network.service.virtualrouter.VirtualRouterCommands
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.*
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.virtualrouter.VirtualRouterNetworkServiceEnv
import org.zstack.testlib.*

/**
 * Created by shixin on 2021/07/23.
 */
class VirtualRouterVmNicEipCase extends SubCase {
    EnvSpec env

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
        env = VirtualRouterNetworkServiceEnv.oneVmOneHostVyosOnServicesEnv()
    }

    @Override
    void test() {
        env.create {
            testVmNicEip()
        }
    }

    void testVmNicEip() {
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory
        L3NetworkInventory pub = env.inventoryByName("pubL3") as L3NetworkInventory
        VirtualRouterVmInventory vrvm = queryVirtualRouterVm {}[0]

        VmNicInventory nic = createVmNic {
            l3NetworkUuid = l3.uuid
        }

        VipInventory vip = createVip {
            name = "test-vip"
            l3NetworkUuid = pub.uuid
        }

        EipInventory eip = createEip {
            name = "eip"
            vipUuid = vip.uuid
        }

        VirtualRouterCommands.CreateEipCmd cmd = null
        env.afterSimulator(VirtualRouterConstant.VR_CREATE_EIP) { rsp, HttpEntity<String> entity ->
            cmd = json(entity.getBody(), VirtualRouterCommands.CreateEipCmd)
            return rsp
        }
        attachEip {
            eipUuid = eip.uuid
            vmNicUuid = nic.uuid
        }
        assert cmd != null
        assert cmd.eip.guestIp == nic.ip
        assert cmd.eip.vipIp == vip.ip

        VirtualRouterCommands.SyncEipCmd scmd = null
        env.afterSimulator(VirtualRouterConstant.VR_SYNC_EIP) { rsp, HttpEntity<String> entity ->
            scmd = json(entity.getBody(), VirtualRouterCommands.SyncEipCmd)
            return rsp
        }

        reconnectVirtualRouter {
            vmInstanceUuid = vrvm.uuid
        }
        assert scmd != null
        assert scmd.eips.size() == 1
        assert scmd.eips.get(0).vipIp == vip.ip
        assert scmd.eips.get(0).guestIp == nic.ip

        deleteVmNic {
            uuid = nic.uuid
        }
    }

}
