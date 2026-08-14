package org.zstack.test.integration.storage.primary.local

import org.springframework.http.HttpEntity
import org.zstack.core.cloudbus.EventCallback
import org.zstack.core.cloudbus.EventFacade
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.SQL
import org.zstack.header.identity.AccessLevel
import org.zstack.header.identity.AccountConstant
import org.zstack.header.identity.AccountResourceRefVO
import org.zstack.header.identity.AccountResourceRefVO_
import org.zstack.header.vm.VmCanonicalEvents
import org.zstack.header.vm.VmInstanceConstant
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMSecurityGroupBackend
import org.zstack.sdk.*
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by miao on 17-5-7.
 */
class LiveMigrateVmCase extends SubCase {
    EnvSpec env
    DatabaseFacade dbf
    EventFacade evtf

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.localStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            testLiveMigrateVmFailure()
            testLiveMigrateVmWithDataVolume()
            testPausedVmStateAfterMigrate()
            testVmStateChangedEventAfterMigrateFailure()
        }
    }
    void testLiveMigrateVmFailure() {
        dbf = bean(DatabaseFacade.class)
        VmInstanceInventory vm1 = (VmInstanceInventory) env.inventoryByName("vm")
        KVMHostInventory host1 = (KVMHostInventory) env.inventoryByName("kvm")
        KVMHostInventory host2 = (KVMHostInventory) env.inventoryByName("kvm1")
        env.simulator(KVMSecurityGroupBackend.SECURITY_GROUP_CLEANUP_UNUSED_RULE_ON_HOST_PATH) {
            return new KVMAgentCommands.CleanupUnusedRulesOnHostResponse()
        }

        assert vm1.hostUuid == host1.uuid
        stopVmInstance {
            uuid = vm1.uuid
        }
        MigrateVmAction action = new MigrateVmAction()
        action.hostUuid = host2.uuid
        action.vmInstanceUuid = vm1.uuid
        action.sessionId = adminSession()
        assert null != action.call().error
    }

    void testLiveMigrateVmWithDataVolume() {
        VmInstanceInventory vm1 = (VmInstanceInventory) env.inventoryByName("vm")

        GetVmCapabilitiesResult capRes = getVmCapabilities {
            uuid = vm1.getUuid()
        } as GetVmCapabilitiesResult

        assert !capRes.capabilities.get(VmInstanceConstant.Capability.LiveMigration.toString()) as Boolean
        assert capRes.capabilities.get(VmInstanceConstant.Capability.VolumeMigration.toString()) as Boolean

        startVmInstance {
            uuid = vm1.uuid
        }
        def invs = queryHost {
        } as List<HostInventory>
        def targetHostUuid = invs.find { i -> i.uuid != vm1.getHostUuid() }.getUuid()

        // default true
        capRes = getVmCapabilities {
            uuid = vm1.getUuid()
        } as GetVmCapabilitiesResult
        assert capRes.capabilities.get(VmInstanceConstant.Capability.LiveMigration.toString()) as Boolean
        assert !capRes.capabilities.get(VmInstanceConstant.Capability.VolumeMigration.toString()) as Boolean

        // record create empty volume cmd
        LocalStorageKvmBackend.CreateEmptyVolumeCmd cmd = null
        env.afterSimulator(LocalStorageKvmBackend.CREATE_EMPTY_VOLUME_PATH) {
            LocalStorageKvmBackend.CreateEmptyVolumeRsp rsp, HttpEntity<String> e ->
                cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.CreateEmptyVolumeCmd.class)
                rsp.success = true
                return rsp
        }

        // share resource, change owner
        AccountInventory account = (AccountInventory) createAccount {
            name = "test"
            password = "test"
        }
        shareResource {
            resourceUuids = Arrays.asList(vm1.getDefaultL3NetworkUuid())
            toPublic = true
        }
        changeResourceOwner {
            resourceUuid = vm1.getUuid()
            accountUuid = account.getUuid()
        }

        // migrate vm
        migrateVm {
            vmInstanceUuid = vm1.getUuid()
            hostUuid = targetHostUuid
        }

        logger.info("XXX: querying vm state")  // to log the timestamp
        List<VmInstanceInventory> qVmInv = queryVmInstance {
            conditions=["uuid=${vm1.getUuid()}".toString()]
        } as List<VmInstanceInventory>

        assert qVmInv[0].state == VmInstanceState.Running.toString()

        // make sure migration success
        retryInSecs {
            VmInstanceVO vmInstanceVO = dbf.findByUuid(vm1.getUuid(), VmInstanceVO.class)
            assert vmInstanceVO.hostUuid == targetHostUuid
        }

        // make sure path keep same
        retryInSecs {
            assert cmd.installUrl.contains(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)
            assert !cmd.installUrl.contains(account.uuid)
        }

        SQL.New(AccountResourceRefVO.class)
                .eq(AccountResourceRefVO_.type, AccessLevel.SharePublic)
                .hardDelete()
    }

    void testPausedVmStateAfterMigrate() {
        VmInstanceInventory vm = (VmInstanceInventory) env.inventoryByName("vm")
        env.afterSimulator(KVMConstant.KVM_VM_CHECK_STATE) { KVMAgentCommands.CheckVmStateRsp rsp, HttpEntity<String> e ->
            if (rsp.states.containsKey(vm.uuid)) {
                rsp.states.put(vm.uuid, KVMConstant.KvmVmState.Paused.toString())
            }
            return rsp
        }

        vm = migrateVm {
            vmInstanceUuid = vm.getUuid()
        } as VmInstanceInventory
        assert vm.state == KVMConstant.KvmVmState.Paused.toString()
    }

    void testVmStateChangedEventAfterMigrateFailure() {
        evtf = bean(EventFacade.class)
        VmInstanceInventory vm = (VmInstanceInventory) env.inventoryByName("vm")
        env.afterSimulator(KVMConstant.KVM_CHECK_L2NOVLAN_NETWORK_PATH) { HttpEntity<String> e ->
            def rsp = new KVMAgentCommands.AgentResponse()
            rsp.success = false
            return rsp
        }

        def fired = false
        evtf.on(VmCanonicalEvents.VM_FULL_STATE_CHANGED_PATH, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                VmCanonicalEvents.VmStateChangedData d = (VmCanonicalEvents.VmStateChangedData) data
                if (d.oldState == VmInstanceState.Migrating.toString()) {
                    fired = true
                }
            }
        })

        expect(AssertionError.class) {
            vm = migrateVm {
                vmInstanceUuid = vm.getUuid()
            } as VmInstanceInventory
        }

        retryInSecs {
            assert fired == true
        }
    }
}
