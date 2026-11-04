package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmSystemTags
import org.zstack.core.cloudbus.CloudBusGlobalConfig
import org.zstack.core.db.Q
import org.zstack.header.image.ImageConstant
import org.zstack.header.vm.VmCreationStrategy
import org.zstack.header.vm.VmInstanceEO
import org.zstack.header.vm.VmInstanceEO_
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.volume.VolumeEO
import org.zstack.header.volume.VolumeVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.*
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil
/**
 * Created by xing5 on 2017/2/22.
 */
class OneVmBasicLifeCycleCase extends SubCase {
    EnvSpec env

    VmInstanceInventory vm
    ImageInventory image
    L3NetworkInventory l3, pubL3
    HostInventory kvm

    def DOC = """
test a VM's start/stop/reboot/destroy/recover operations 
"""

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        CloudBusGlobalConfig.STATISTICS_ON.updateValue(true)

        env = Env.oneVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
            prepare()
            testStopVm()
            testStartVm()
            testRebootVm()
            testDestroyVm()
            testRecoverVm()
            testExpungeVm()            
            testDeleteCreatedVm()
            testFailToCreateVmInDisabledZone()
            testFailToCreateVmInDisabledCluster()
            testFailToCreateVmInDisabledHost()
            testCreateVmParameter()
        }
    }

    void prepare() {
        vm = env.inventoryByName("vm") as VmInstanceInventory

        boolean vmHasVirtio = VmSystemTags.VIRTIO.hasTag(vm.uuid)
        assert vmHasVirtio

        image = env.inventoryByName("image1") as ImageInventory
        l3 = env.inventoryByName("l3") as L3NetworkInventory
        pubL3 = env.inventoryByName("pubL3") as L3NetworkInventory
        kvm = env.inventoryByName("kvm") as HostInventory
    }

    void testRecoverVm() {
        def inv = recoverVmInstance {
            uuid = vm.uuid
        } as VmInstanceInventory

        assert inv.state == VmInstanceState.Stopped.toString()

        // confirm the vm can start after being recovered
        testStartVm()
    }

    void testDestroyVm() {
        KVMAgentCommands.DestroyVmCmd cmd = null

        env.afterSimulator(KVMConstant.KVM_DESTROY_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.DestroyVmCmd.class)
            return rsp
        }

        destroyVmInstance {
            uuid = vm.uuid
        }

        assert cmd != null
        assert cmd.uuid == vm.uuid
        VmInstanceVO vmvo = dbFindByUuid(cmd.uuid, VmInstanceVO.class)
        assert vmvo.state == VmInstanceState.Destroyed
    }

    void testRebootVm() {
        // reboot = stop + start
        KVMAgentCommands.StartVmCmd startCmd = null
        KVMAgentCommands.StopVmCmd stopCmd = null

        env.afterSimulator(KVMConstant.KVM_STOP_VM_PATH) { rsp, HttpEntity<String> e ->
            stopCmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StopVmCmd.class)
            return rsp
        }

        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            startCmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StartVmCmd.class)
            return rsp
        }

        def inv = rebootVmInstance {
            uuid = vm.uuid
        } as VmInstanceInventory

        assert startCmd != null
        assert startCmd.vmInstanceUuid == vm.uuid
        assert startCmd.chassisAssetTag == "www.zstack.io"
        assert stopCmd != null
        assert stopCmd.uuid == vm.uuid
        assert inv.state == VmInstanceState.Running.toString()
    }

    void testStartVm() {
        KVMAgentCommands.StartVmCmd cmd = null

        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StartVmCmd.class)
            return rsp
        }

        def inv = startVmInstance {
            uuid = vm.uuid
        } as VmInstanceInventory

        assert cmd != null
        assert cmd.vmInstanceUuid == vm.uuid
        assert inv.state == VmInstanceState.Running.toString()
        assert cmd.chassisAssetTag == "www.zstack.io"

        VmInstanceVO vmvo = dbFindByUuid(cmd.vmInstanceUuid, VmInstanceVO.class)
        assert vmvo.state == VmInstanceState.Running
        assert cmd.vmInternalId == vmvo.internalId
        assert cmd.vmName == vmvo.name
        assert cmd.memory == vmvo.memorySize
        assert cmd.cpuNum == vmvo.cpuNum

        String tag = VmSystemTags.VM_SYSTEM_SERIAL_NUMBER.getTag(vm.uuid)
        assert tag != null

        assert cmd.rootVolume.installPath == vmvo.rootVolume.installPath
        assert cmd.rootVolume.useVirtio
        vmvo.vmNics.each { nic ->
            KVMAgentCommands.NicTO to = cmd.nics.find { nic.mac == it.mac }
            assert to != null: "unable to find the nic[mac:${nic.mac}]"
            assert to.deviceId == nic.deviceId
            assert to.useVirtio
            assert to.nicInternalName == nic.internalName
        }
    }

    void testStopVm() {
        KVMAgentCommands.StopVmCmd cmd = null

        env.afterSimulator(KVMConstant.KVM_STOP_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StopVmCmd.class)
            return rsp
        }

        def inv = stopVmInstance {
            uuid = vm.uuid
        } as VmInstanceInventory

        assert inv.state == VmInstanceState.Stopped.toString()

        assert cmd != null
        assert cmd.uuid == vm.uuid

        def vmvo = dbFindByUuid(cmd.uuid, VmInstanceVO.class)
        assert vmvo.state == VmInstanceState.Stopped
    }

    void testExpungeVm() {
        destroyVmInstance {
            uuid = vm.uuid
        }

        Long before = Q.New(VmInstanceEO.class).count()

        expungeVmInstance {
            uuid = vm.uuid
        }

        boolean eoExists = Q.New(VmInstanceEO.class)
                .eq(VmInstanceEO_.uuid, vm.uuid)
                .isExists()
        assert !eoExists

        boolean rootVolumeEoExists = Q.New(VolumeEO.class)
                .eq(VolumeVO_.uuid, vm.rootVolumeUuid)
                .isExists()
        assert !rootVolumeEoExists

        Long after = Q.New(VmInstanceEO.class).count()
        assert before == after + 1
    }

    void testFailToCreateVmInDisabledZone() {
        def zoneInventory = env.inventoryByName("zone") as ZoneInventory

        changeZoneState {
            uuid = zoneInventory.uuid
            stateEvent = "disable"
        }

        expectApiFailure({
            createVmInstance {
                delegate.name = "test-for-parameter"
                delegate.zoneUuid = zoneInventory.uuid
                delegate.cpuNum = 4
                delegate.memorySize = SizeUnit.GIGABYTE.toByte(8)
                delegate.imageUuid = image.uuid
                delegate.l3NetworkUuids = [l3.uuid]
                delegate.strategy = VmCreationStrategy.JustCreate.toString()
            }
        }) {
            assert delegate.code == "SYS.1007"
            assert delegate.details.contains(zoneInventory.uuid)
            assert delegate.opaque["zone.uuid"]
            assert delegate.opaque["zone.uuid"] == zoneInventory.uuid
        }

        changeZoneState {
            uuid = zoneInventory.uuid
            stateEvent = "enable"
        }
    }

    void testFailToCreateVmInDisabledCluster() {
        def clusterInventory = env.inventoryByName("cluster") as ClusterInventory

        changeClusterState {
            uuid = clusterInventory.uuid
            stateEvent = "disable"
        }

        expectApiFailure({
            createVmInstance {
                delegate.name = "test-for-parameter"
                delegate.clusterUuid = clusterInventory.uuid
                delegate.cpuNum = 4
                delegate.memorySize = SizeUnit.GIGABYTE.toByte(8)
                delegate.imageUuid = image.uuid
                delegate.l3NetworkUuids = [l3.uuid]
                delegate.strategy = VmCreationStrategy.JustCreate.toString()
            }
        }) {
            assert delegate.code == "SYS.1007"
            assert delegate.details.contains(clusterInventory.uuid)
            assert delegate.opaque["cluster.uuid"]
            assert delegate.opaque["cluster.uuid"] == clusterInventory.uuid
        }

        changeClusterState {
            uuid = clusterInventory.uuid
            stateEvent = "enable"
        }
    }

    void testFailToCreateVmInDisabledHost() {
        changeHostState {
            uuid = kvm.uuid
            stateEvent = "disable"
        }

        expect(AssertionError.class) {
            createVmInstance {
                delegate.name = "test-for-parameter"
                delegate.hostUuid = kvm.uuid
                delegate.cpuNum = 4
                delegate.memorySize = SizeUnit.GIGABYTE.toByte(8)
                delegate.imageUuid = image.uuid
                delegate.l3NetworkUuids = [l3.uuid]
                delegate.strategy = VmCreationStrategy.JustCreate.toString()
            }
        }

        changeHostState {
            uuid = kvm.uuid
            stateEvent = "enable"
        }
    }

    void testCreateVmParameter() {
        //without default l3 network
        expect(AssertionError.class) {
            createVmInstance {
                name = "test-for-parameter"
                hostUuid = kvm.uuid
                cpuNum = 4
                memorySize = SizeUnit.GIGABYTE.toByte(8)
                imageUuid = image.uuid
                l3NetworkUuids = [l3.uuid, pubL3.uuid]
                strategy = VmCreationStrategy.JustCreate.toString()
            }
        }

        //with default l3, but it is not in l3NetworkUuids
        expect(AssertionError.class) {
            createVmInstance {
                name = "test-for-parameter"
                hostUuid = kvm.uuid
                cpuNum = 4
                memorySize = SizeUnit.GIGABYTE.toByte(8)
                imageUuid = image.uuid
                l3NetworkUuids = [l3.uuid]
                defaultL3NetworkUuid = pubL3.uuid
                strategy = VmCreationStrategy.JustCreate.toString()
            }
        }

        //with default l3, but it is not in l3NetworkUuids
        expect(AssertionError.class) {
            createVmInstance {
                name = "test-for-parameter"
                hostUuid = kvm.uuid
                cpuNum = 4
                memorySize = SizeUnit.GIGABYTE.toByte(8)
                imageUuid = image.uuid
                l3NetworkUuids = [l3.uuid]
                defaultL3NetworkUuid = pubL3.uuid
                strategy = VmCreationStrategy.JustCreate.toString()
            }
        }

        //image
        def bs = env.inventoryByName("sftp") as BackupStorageInventory
        def data = addImage {
            name = "image-data-volume"
            mediaType = ImageConstant.ImageMediaType.DataVolumeTemplate
            url = "http://zstack.org/download/test-volume.qcow2"
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.QCOW2_FORMAT_STRING
            system = true
        } as ImageInventory

        def iso = addImage {
            name = "sized-image"
            url = "http://my-site/foo.iso"
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.ISO_FORMAT_STRING
        } as ImageInventory

        expect(AssertionError.class) {
            createVmInstance {
                name = "test-for-parameter"
                hostUuid = kvm.uuid
                cpuNum = 4
                memorySize = SizeUnit.GIGABYTE.toByte(8)
                imageUuid = data.uuid
                l3NetworkUuids = [l3.uuid]
                strategy = VmCreationStrategy.JustCreate.toString()
            }
        }

        expect(AssertionError.class) {
            createVmInstance {
                name = "test-for-parameter"
                hostUuid = kvm.uuid
                cpuNum = 4
                memorySize = SizeUnit.GIGABYTE.toByte(8)
                imageUuid = iso.uuid
                l3NetworkUuids = [l3.uuid]
                strategy = VmCreationStrategy.JustCreate.toString()
            }
        }

        expect(AssertionError.class) {
            createVmInstance {
                name = "test-for-parameter"
                hostUuid = kvm.uuid
                cpuNum = 4
                memorySize = SizeUnit.GIGABYTE.toByte(8)
                imageUuid = data.uuid
                l3NetworkUuids = [l3.uuid]
                strategy = VmCreationStrategy.JustCreate.toString()
                diskAOs = [
                    [
                        boot: true,
                        size: SizeUnit.GIGABYTE.toByte(20)
                    ]
                ]
            }
        }

    }

    void testDeleteCreatedVm() {
        def justCreatedVm = createVmInstance {
            delegate.name = "JustCreatedVm"
            delegate.cpuNum = 4
            delegate.memorySize = SizeUnit.GIGABYTE.toByte(8)
            delegate.imageUuid = image.uuid
            delegate.l3NetworkUuids = [l3.uuid]
            delegate.strategy = VmCreationStrategy.JustCreate.toString()
            delegate.diskAOs = [
                [
                    boot: true,
                    size: SizeUnit.GIGABYTE.toByte(20)
                ]
            ]
        } as VmInstanceInventory

        destroyVmInstance {
            uuid = justCreatedVm.uuid
        }

        VmInstanceVO vo = dbFindByUuid(justCreatedVm.uuid, VmInstanceVO.class)
        assert vo == null
    }

    @Override
    void clean() {
        env.delete()
    }
}
