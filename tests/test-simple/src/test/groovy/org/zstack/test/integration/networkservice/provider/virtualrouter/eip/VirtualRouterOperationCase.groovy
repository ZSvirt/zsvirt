package org.zstack.test.integration.networkservice.provider.virtualrouter.eip

import org.zstack.appliancevm.ApplianceVmVO
import org.zstack.core.db.DatabaseFacade
import org.springframework.http.HttpEntity
import org.zstack.header.vm.VmNicVO
import org.zstack.sdk.ApplianceVmInventory
import org.zstack.core.db.Q
import org.zstack.network.service.virtualrouter.VirtualRouterCommands
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO_
import org.zstack.sdk.EipInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.virtualrouter.VirtualRouterNetworkServiceEnv
import org.zstack.testlib.EipSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.L3NetworkSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.VmSpec
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by Camile on 2017/3/24.
 */

class VirtualRouterOperationCase extends SubCase {
    EnvSpec env

    VmInstanceInventory vm
    L3NetworkInventory publicL3
    EipInventory eip

    @Override
    void test() {
        env.create {
            vm = (env.specByName("vm") as VmSpec).inventory
            eip = (env.specByName("eip") as EipSpec).inventory
            publicL3 = (env.specByName("pubL3") as L3NetworkSpec).inventory

            testVirtualRouterReconnect()
            testUpdateGlobalConfigAndPingVirtualRouter()
            testQueryVirtualRouterVm()
            testQueryVRImage()
        }
    }

    void testQueryVRImage() {
        ImageInventory vr = env.inventoryByName("vr")

        List<ImageInventory> images = queryImage {
            conditions = ["system=true"]
        }

        assert images.size() == 1
        assert images[0].uuid == vr.uuid
        assert images[0].system

        images = queryImage {
            conditions = ["system=false"]
        }

        assert !images.any { it.system }
    }

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
        env = VirtualRouterNetworkServiceEnv.oneVmOneHostVyosOnEipEnv()
    }

    void testQueryVirtualRouterVm() {
        ApplianceVmVO vo = Q.New(ApplianceVmVO.class).list()[0]
        assert vo

        VmNicVO nic = vo.getVmNics()[0]
        List lst = queryApplianceVm { conditions=["vmNics.l3NetworkUuid=${nic.l3NetworkUuid}"] }
        assert !lst.isEmpty()
    }

    void testVirtualRouterReconnect() {
        DatabaseFacade dbf = bean(DatabaseFacade.class)
        ApplianceVmVO vr = dbf.listAll(ApplianceVmVO.class).get(0)
        VmInstanceInventory vm = env.inventoryByName("vm")
        EipInventory eip = env.inventoryByName("eip")

        VmNicInventory nic1 = vm.getVmNics().get(0)
        attachEip {
            eipUuid = eip.uuid
            vmNicUuid = nic1.uuid
        }

        VirtualRouterCommands.CreateVipCmd vipCmd = null
        env.afterSimulator(VirtualRouterConstant.VR_CREATE_VIP) { rsp, HttpEntity<String> entity ->
            vipCmd = JSONObjectUtil.toObject(entity.body, VirtualRouterCommands.CreateVipCmd.class)
            assert vipCmd.vips.size() == 2
            assert vipCmd.vips.get(0).vipUuid == eip.vipUuid || vipCmd.vips.get(1).vipUuid
            return rsp
        }

        ApplianceVmInventory inv = reconnectVirtualRouter {
            vmInstanceUuid = vr.uuid
            sessionId = adminSession()
        }
    }


    void testUpdateGlobalConfigAndPingVirtualRouter() {
        assert (Q.New(VirtualRouterVmVO.class).select(VirtualRouterVmVO_.uuid).listValues().get(0)) != null
        updateGlobalConfig {
            name = "ping.interval"
            category = "virtualRouter"
            value = 1
            sessionId = adminSession()
        }

        VirtualRouterCommands.PingCmd cmd = null
        env.afterSimulator(VirtualRouterConstant.VR_PING) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, VirtualRouterCommands.PingCmd.class)
            return rsp
        }

        env.simulator(VirtualRouterConstant.VR_PING) { HttpEntity<String> e, EnvSpec spec ->
            def rsp = new VirtualRouterCommands.PingRsp()
            rsp.success = true
            return rsp
        }
        VirtualRouterCommands.InitCommand icmd = null
        env.afterSimulator(VirtualRouterConstant.VR_INIT) { rsp, HttpEntity<String> e ->
            icmd = JSONObjectUtil.toObject(e.body, VirtualRouterCommands.InitCommand.class)
            return rsp
        }

        retryInSecs(10, 1) {
            assert cmd != null
        }

        retryInSecs(10, 1) {
            assert icmd != null
        }
    }
}
