package org.zstack.test.integration.networkservice.provider.virtualrouter.eip

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.exception.CloudRuntimeException
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO_
import org.zstack.header.vm.VmNicVO
import org.zstack.header.vm.VmNicVO_
import org.zstack.network.service.eip.EipVO
import org.zstack.network.service.vip.*
import org.zstack.network.service.virtualrouter.VirtualRouterCommands
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.*
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.virtualrouter.VirtualRouterNetworkServiceEnv
import org.zstack.testlib.*
import org.zstack.utils.CollectionUtils
import org.zstack.utils.gson.JSONObjectUtil

import javax.persistence.Tuple

/**
 * Created by xing5 on 2017/3/7.
 */
class VirtualRouterEipCase extends SubCase {
    EnvSpec env

    VmInstanceInventory vm
    L3NetworkInventory publicL3
    EipInventory eip
    EipInventory eip1

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

    void testDetachEipJustAfterAttachToStoppedVm() {
        VirtualRouterVmInventory vrvm = queryVirtualRouterVm {}[0]

        VirtualRouterCommands.CreateVipCmd vipCmd = null
        env.afterSimulator(VirtualRouterConstant.VR_CREATE_VIP) { rsp, HttpEntity<String> entity ->
            vipCmd = JSONObjectUtil.toObject(entity.body, VirtualRouterCommands.CreateVipCmd.class)
            return rsp
        }

        VirtualRouterCommands.CreateEipCmd cmd = new VirtualRouterCommands.CreateEipCmd()
        env.afterSimulator(VirtualRouterConstant.VR_CREATE_EIP) { rsp, HttpEntity<String> entity ->
            cmd = json(entity.getBody(), VirtualRouterCommands.CreateEipCmd)
            return rsp
        }
        attachEip {
            eipUuid = eip1.uuid
            vmNicUuid = vm.vmNics[0].uuid
        }
        assert cmd.eip.publicMac.equals(vrvm.vmNics.stream().filter{ nic ->
            nic.l3NetworkUuid.equals(publicL3.uuid)
        }.findFirst().get().mac)
        assert vipCmd.vips.size() == 1
        assert vipCmd.vips.get(0).vipUuid == eip1.vipUuid

        VirtualRouterCommands.RemoveVipCmd removeVipCmd = null
        env.afterSimulator(VirtualRouterConstant.VR_REMOVE_VIP) { rsp, HttpEntity<String> entity ->
            removeVipCmd = json(entity.getBody(), VirtualRouterCommands.RemoveVipCmd)
            return rsp
        }

        detachEip {
            uuid = eip1.uuid
        }

        assert removeVipCmd != null

        stopVmInstance {
            uuid = vm.uuid
        }

        attachEip {
            eipUuid = eip.uuid
            vmNicUuid = vm.vmNics[0].uuid
        }

        detachEip {
            uuid = eip.uuid
        }

        EipVO vo = dbFindByUuid(eip.uuid, EipVO.class)
        assert vo.vmNicUuid == null

        VipVO vip = dbFindByUuid(eip.vipUuid, VipVO.class)
        // the vip has not created on backend
        assert vip.serviceProvider == null
        assert Q.New(VipPeerL3NetworkRefVO.class).eq(VipPeerL3NetworkRefVO_.vipUuid, vip.uuid).count() == 0
        assert Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vip.uuid).count() == 1
    }

    void testVirtualRouterDHCP() {
        def defaultNic = CollectionUtils.findOneOrNull(vm.getVmNics(), { VmNicInventory it -> it.l3NetworkUuid == vm.defaultL3NetworkUuid })
        assert defaultNic != null

        final List<String> l3s = CollectionUtils.transformAndRemoveNull(vm.getVmNics(),
                { VmNicInventory nic -> nic.getL3NetworkUuid() })
        long count = Q.New(NetworkServiceL3NetworkRefVO.class).select()
                .eq(NetworkServiceL3NetworkRefVO_.networkServiceType, "DHCP")
                .in(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, l3s).count()
        assert count == 1

        Tuple tuple = Q.New(VmNicVO.class).select(VmNicVO_.mac, VmNicVO_.ip).eq(VmNicVO_.uuid, defaultNic.uuid).findTuple()
        assert defaultNic.mac == tuple.get(0, String.class)
        assert defaultNic.ip == tuple.get(1, String.class)
    }

    void testDeleteEipOnRevokeEipFailure() {
        startVmInstance {
            uuid = vm.uuid
        }

        attachEip {
            eipUuid = eip.uuid
            vmNicUuid = vm.vmNics[0].uuid
        }

        env.afterSimulator(VirtualRouterConstant.VR_REMOVE_EIP) {
            rsp, HttpEntity<String> e -> throw new CloudRuntimeException("injected fault")
        }

        // TODO: once we add GC to eip, the delete will success
        // then, fix the case
        expect(AssertionError.class) {
            deleteEip {
                uuid = eip.uuid
            }
        }

        assert dbIsExists(eip.uuid, EipVO.class)
        env.cleanAfterSimulatorHandlers()
    }

    @Override
    void test() {
        env.create {
            vm = (env.specByName("vm") as VmSpec).inventory
            eip = (env.specByName("eip") as EipSpec).inventory
            eip1 = (env.specByName("eip1") as EipSpec).inventory
            publicL3 = (env.specByName("pubL3") as L3NetworkSpec).inventory
            testDetachEipJustAfterAttachToStoppedVm()
            testDeleteEipOnRevokeEipFailure()
            testVirtualRouterDHCP()
        }
    }

}
