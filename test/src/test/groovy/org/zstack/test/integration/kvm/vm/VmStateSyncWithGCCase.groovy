package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.core.gc.GCStatus
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.GarbageCollectorInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class VmStateSyncWithGCCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneVmBasicEnv()

    }

    @Override
    void test() {
        env.create {
            testDestroyVmAndRecoverItTheStateOfVmIsStopped()
        }
    }

    void testDestroyVmAndRecoverItTheStateOfVmIsStopped() {
        /*
        * 1. reconnect host
        * 2. destroy & recover vm during the host connecting
        * 3. the vm state should be stopped after the host connect success
        * 4. the GC job will delete the vm in the host after the host connect success
        * */
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory
        String hostUuid = vm.getHostUuid()
        VmInstanceVO vmVO
        HostVO hostVO

        //monitor the reconnect host cmd and make sure that it will not finish until the vm finishes to recover
        AtomicBoolean vmRecovered = new AtomicBoolean(false)
        env.afterSimulator(KVMConstant.KVM_CONNECT_PATH) { KVMAgentCommands.ConnectResponse rsp, HttpEntity<String> e ->
            rsp.success = true
            while (!vmRecovered.get()) {
                TimeUnit.MILLISECONDS.sleep(100)
            }

            return rsp
        }

        //the wm sync will be triggered while the host is connected, the related GC task should be run
        env.afterSimulator(KVMConstant.KVM_VM_SYNC_PATH) { KVMAgentCommands.VmSyncResponse rsp ->
            GarbageCollectorInventory gcinv
            gcinv = queryGCJob {
                conditions=["context~=%${vm.uuid}%"]
            }[0]

            if (gcinv != null && gcinv.status != GCStatus.Done.toString()) {
                rsp.states[(vm.uuid)] = KVMConstant.KvmVmState.Running.toString()
            }
            return rsp
        }

        KVMAgentCommands.DestroyVmCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_DESTROY_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, KVMAgentCommands.DestroyVmCmd.class)
            return rsp
        }

        Thread.start {
            reconnectHost {
                uuid = hostUuid
            }
        }

        retryInSecs {
            assert dbFindByUuid(hostUuid, HostVO.class).status == HostStatus.Connecting
        }

        //destroy & recover vm while the host is connecting
        destroyVmInstance {
            uuid = vm.uuid
        }
        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).state == VmInstanceState.Destroyed
        assert cmd == null

        GarbageCollectorInventory inv = null
        retryInSecs {
            inv = queryGCJob {
                conditions=["context~=%${vm.uuid}%"]
            }[0]

            assert inv.status == GCStatus.Idle.toString()
        }
        recoverVmInstance {
            uuid = vm.getUuid()
        }

        retryInSecs {
            inv = queryGCJob {
                conditions=["context~=%${vm.uuid}%"]
            }[0]

            assert inv.status == GCStatus.Idle.toString()
        }

        assert dbFindByUuid(hostUuid, HostVO.class).status == HostStatus.Connecting
        //set the flag to finish the host re-connect cmd
        vmRecovered.getAndSet(true)

        //the GC job will delete the vm in the host after the host connect success
        retryInSecs {
            assert cmd != null
            assert cmd.uuid == vm.uuid
            inv = queryGCJob {
                conditions=["context~=%${vm.uuid}%"]
            }[0]

            assert inv.status == GCStatus.Done.toString()
        }

        //the vm state should be stopped after the host re-connect
        retryInSecs {
            hostVO = dbFindByUuid(hostUuid, HostVO.class)
            vmVO = dbFindByUuid(vm.getUuid(), VmInstanceVO.class)

            assert hostVO.status == HostStatus.Connected
            assert vmVO.state == VmInstanceState.Stopped
            assert vmVO.getHostUuid() == null
        }

    }
}
