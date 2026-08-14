package org.zstack.test.integration.kvm.host

import org.springframework.http.HttpEntity
import org.zstack.compute.host.HostGlobalConfig
import org.zstack.compute.host.HostMaintenancePolicyManager
import org.zstack.core.db.Q
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HttpError
import org.zstack.testlib.SubCase

/**
 * Created by kayo on 2018/11/27.
 */

/*
 * two hosts with nfs primary storage
 * one vm on host1, no vm on host2
 * 1. change host maintenance policy to JustMigrate
 * 2. make vm migrate fail and host1 failed to change to maintenance mode
 * 3. change host maintenance policy to StopVmOnMigrationFailure
 * 4. make vm migrate fail
 * 5. vm will stopped and host successfully enter maintenance mode
 */
class HostMaintenancePolicyCase extends SubCase {
    EnvSpec env
    boolean migrateVmFail = false

    @Override
    void test() {
        env.create {
            prepareSimulator()
            testMaintenanceJustMigrateVmPolicy()
            testMaintenanceHostStopVmOnMigrationFailure()
        }
    }

    void testMaintenanceHostStopVmOnMigrationFailure() {
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory

        migrateVmFail = true

        changeHostState {
            uuid = vm.hostUuid
            stateEvent = "maintain"
        }

        // confirm the vm is still running
        assert Q.New(VmInstanceVO.class).eq(VmInstanceVO_.state, VmInstanceState.Stopped).isExists()

        recoverEnv()
    }

    void testMaintenanceJustMigrateVmPolicy() {
        VmInstanceInventory vm = env.inventoryByName("vm") as VmInstanceInventory

        HostGlobalConfig.HOST_MAINTENANCE_POLICY.updateValue(HostMaintenancePolicyManager.HostMaintenancePolicy.JustMigrate.toString())

        migrateVmFail = true

        expect(AssertionError.class) {
            changeHostState {
                uuid = vm.hostUuid
                stateEvent = "maintain"
            }
        }

        // confirm the vm is still running
        assert Q.New(VmInstanceVO.class).eq(VmInstanceVO_.state, VmInstanceState.Running).isExists()

        recoverEnv()
    }

    void recoverEnv() {
        migrateVmFail = false
        HostGlobalConfig.HOST_MAINTENANCE_POLICY.updateValue(HostMaintenancePolicyManager.HostMaintenancePolicy.StopVmOnMigrationFailure.toString())
    }

    void prepareSimulator() {
        env.afterSimulator(KVMConstant.KVM_MIGRATE_VM_PATH) { rsp, HttpEntity<String> e ->
            if (migrateVmFail) {
                throw new HttpError(503, "on purpose")
            }

            return rsp
        }
    }

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
        env = Env.oneVmTwoHostNfsEnv()
    }
}
