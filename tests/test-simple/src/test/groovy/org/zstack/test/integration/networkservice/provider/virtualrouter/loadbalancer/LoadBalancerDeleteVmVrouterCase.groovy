package org.zstack.test.integration.networkservice.provider.virtualrouter.loadbalancer

import org.springframework.http.HttpEntity
import org.zstack.header.vm.VmInstanceState
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerBackend
import org.zstack.sdk.*
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.virtualrouter.VirtualRouterNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.controller.VRouterController
import org.zstack.utils.gson.JSONObjectUtil
/**
 * @author: zhanyong.miao
 * @date: 2019-12-20
 * */
class LoadBalancerDeleteVmVrouterCase extends SubCase {
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
        env = VirtualRouterNetworkServiceEnv.oneVmOneHostVyosOnServicesEnv()
    }

    @Override
    void test() {
        env.create {
            testDeleteVmAndRouter()
        }
    }

    void testDeleteVmAndRouter() {
        L3NetworkInventory pubL3 = env.inventoryByName("pubL3")
        VmInstanceInventory vm = env.inventoryByName("vm")
        VirtualRouterVmInventory vr = queryVirtualRouterVm {}[0]

        VipInventory vip = createVip {
            name = "test-vip"
            l3NetworkUuid = pubL3.uuid
        }
        LoadBalancerInventory lb = createLoadBalancer {
            name = "test-lb"
            vipUuid = vip.uuid
        }

        def listener = createLoadBalancerListener {
            loadBalancerUuid = lb.uuid
            loadBalancerPort = 100
            instancePort = 100
            name = "test-listener-1"
        } as LoadBalancerListenerInventory

        addVmNicToLoadBalancer {
            vmNicUuids = [vm.getVmNics().get(0).uuid]
            listenerUuid = listener.uuid
        }

        def controller = new VRouterController(env)
        controller.connect(vr.uuid)

        List<VirtualRouterLoadBalancerBackend.RefreshLbCmd> cmds = new ArrayList<>()
        env.afterSimulator(VirtualRouterLoadBalancerBackend.REFRESH_LB_PATH) { rsp, HttpEntity<String> e ->
            VirtualRouterLoadBalancerBackend.RefreshLbCmd cmd =
                    JSONObjectUtil.toObject(e.body, VirtualRouterLoadBalancerBackend.RefreshLbCmd.class)
            vr = queryVirtualRouterVm {}[0]
            if (!vr.state.equals(VmInstanceState.Running.toString())) {
                throw new Exception("on purpose")
            }
            cmds.add(cmd)
            return rsp
        }

        KVMAgentCommands.DestroyVmCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_DESTROY_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, KVMAgentCommands.DestroyVmCmd.class)
            if (cmd.uuid.equals(vr.uuid)) {
                sleep(2000)
            }
            return rsp
        }


        def thread1 = Thread.start {
            expect(ApiException.class) {
                destroyVmInstance {
                    uuid = vm.uuid
                }
            }
        }

        def thread2 = Thread.start {
            destroyVmInstance {
                uuid = vr.uuid
            }
        }
        [thread1, thread2].each {it.join()}

        assert cmds.size() == 0

        LoadBalancerInventory lb1 = queryLoadBalancer {conditions=["uuid=${lb.uuid}".toString()]} [0]
        assert lb1.listeners.size() == 1

        //delete vm fail
        LoadBalancerListenerInventory lbl1 = lb1.listeners.get(0)
        assert lbl1.serverGroupRefs.size() == 1
    }
}
