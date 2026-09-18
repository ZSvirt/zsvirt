package org.zstack.test.integration.networkservice.provider.virtualrouter

import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.header.vm.VmNicVO
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO
import org.zstack.sdk.IpRangeInventory
import org.zstack.sdk.L2NetworkInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * Created by shixin on 03/30/2018.
 */
class VirtualRouterDeletePublicNetworkCase extends SubCase {

    EnvSpec env
    DatabaseFacade dbf

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = VirtualRouterNetworkServiceEnv.oneVmOneHostVyosOnEipEnv()
    }

    @Override
    void test() {
        env.create {
            testDeleteAdditionalPublicNetwork()
        }
    }

    void testDeleteAdditionalPublicNetwork() {
        def l2 = env.inventoryByName("l2") as L2NetworkInventory
        def pub = env.inventoryByName("pubL3") as L3NetworkInventory

        L3NetworkInventory l3_1 = createL3Network {
            delegate.category = "Public"
            delegate.l2NetworkUuid = l2.uuid
            delegate.name = "pubL3-2"
        }

        addIpRange {
            delegate.name = "TestIpRange"
            delegate.l3NetworkUuid = l3_1.uuid
            delegate.startIp = "11.168.200.10"
            delegate.endIp = "11.168.200.253"
            delegate.gateway = "11.168.200.1"
            delegate.netmask = "255.255.255.0"
        }

        L3NetworkInventory l3_2 = createL3Network {
            delegate.category = "Public"
            delegate.l2NetworkUuid = l2.uuid
            delegate.name = "pubL3-3"
        }

        addIpRange {
            delegate.name = "TestIpRange-2"
            delegate.l3NetworkUuid = l3_2.uuid
            delegate.startIp = "11.168.210.10"
            delegate.endIp = "11.168.210.253"
            delegate.gateway = "11.168.210.1"
            delegate.netmask = "255.255.255.0"
        }

        VirtualRouterVmVO vr = Q.New(VirtualRouterVmVO.class).find()

        attachL3NetworkToVm {
            delegate.l3NetworkUuid = l3_1.getUuid()
            delegate.vmInstanceUuid = vr.uuid
        }

        attachL3NetworkToVm {
            delegate.l3NetworkUuid = l3_2.getUuid()
            delegate.vmInstanceUuid = vr.uuid
        }

        deleteL3Network {
            uuid = l3_1.uuid
        }
        
        vr = Q.New(VirtualRouterVmVO.class).find()
        assert vr != null
        assert vr.getPublicNetworkUuid() == pub.uuid
        assert vr.getManagementNetworkUuid() == pub.uuid
        assert vr.getVmNics().size() == 3
        for (VmNicVO nic : vr.getVmNics()) {
            assert nic.l3NetworkUuid != l3_1.uuid && nic.l3NetworkUuid != null
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
