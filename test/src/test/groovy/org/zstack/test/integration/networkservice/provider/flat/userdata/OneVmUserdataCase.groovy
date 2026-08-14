package org.zstack.test.integration.networkservice.provider.flat.userdata

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmSystemTags
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmInstanceVO
import org.zstack.kvm.KVMConstant
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.BridgeNameFinder
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.flat.FlatNetworkSystemTags
import org.zstack.network.service.flat.FlatUserdataBackend
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.network.service.userdata.UserdataGlobalProperty
import org.zstack.sdk.HostInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.*
import org.zstack.utils.data.SizeUnit

/**
 * Created by xing5 on 2017/2/26.
 */
class OneVmUserdataCase extends SubCase {
    EnvSpec env

    VmInstanceInventory vm
    L3NetworkInventory l3
    String userdata

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image"
                    url = "http://zstack.org/download/test.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "kvm1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                nfsPrimaryStorage {
                    name = "local"
                    url = "127.0.0.1:/nfs"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }
                }

                attachBackupStorage("sftp")
            }
        }
    }

    @Override
    void test() {
        env.create {
            userdata = "this test user data"
            for(int i = 0; i < 10000; i++){
                userdata += 1
            }

            l3 = (env.specByName("l3") as L3NetworkSpec).inventory
            def oldValue =  FlatUserdataBackend.UserdataReleseGC.INTERVAL
            FlatUserdataBackend.UserdataReleseGC.INTERVAL = 1

            testSetUserdataWhenCreateVm()
            testDeleteUserdataWhenStopVm()
            testSetUserdataWhenStartVm()
            testSetAndDeleteUserddataWhenRebootVm()
            testDeleteUserdataWhenDestroyVm()
            testUserdataForMigratedVm()

            FlatUserdataBackend.UserdataReleseGC.INTERVAL = oldValue
        }
    }

    void testDeleteUserdataWhenDestroyVm() {
        testDeleteUserdataSetWhenVmOperations {
            destroyVmInstance {
                uuid = vm.uuid
            }
        }
    }

    void testSetUserdataWhenStartVm() {
        testSetUserdataSetWhenVmOperations {
            startVmInstance {
                uuid = vm.uuid
            }
        }
    }

    void testUserdataForMigratedVm() {
        expectError {
            createVmInstance {
                name = "vm"
                imageUuid = env.inventoryByName("image").uuid
                l3NetworkUuids = [l3.uuid]
                instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
                systemTags = [VmSystemTags.USERDATA.instantiateTag([(VmSystemTags.USERDATA_TOKEN): "测试未Base64_encode"])]
                hostUuid = env.inventoryByName("kvm").uuid
            }
        }

        VmInstanceInventory vm = createVmInstance {
            name = "vm"
            imageUuid = env.inventoryByName("image").uuid
            l3NetworkUuids = [l3.uuid]
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            systemTags = [VmSystemTags.USERDATA.instantiateTag([(VmSystemTags.USERDATA_TOKEN): new String(Base64.getEncoder().encode(userdata.getBytes()))])]
            hostUuid = env.inventoryByName("kvm").uuid
        }

        def testUserdataOnVmMigrationSuccess = {
            def applyCmd = null
            def releaseCmd = null
            env.afterSimulator(FlatUserdataBackend.APPLY_USER_DATA) { rsp, HttpEntity<String> e ->
                FlatUserdataBackend.ApplyUserdataCmd cmd = json(e.body, FlatUserdataBackend.ApplyUserdataCmd.class)
                applyCmd = cmd
                return rsp
            }

            env.afterSimulator(FlatUserdataBackend.RELEASE_USER_DATA) { rsp, HttpEntity<String> e ->
                FlatUserdataBackend.ReleaseUserdataCmd cmd = json(e.body, FlatUserdataBackend.ReleaseUserdataCmd.class)
                releaseCmd = cmd
                return rsp
            }

            HostInventory kvm1 = env.inventoryByName("kvm1")
            migrateVm {
                vmInstanceUuid = vm.uuid
                hostUuid = kvm1.uuid
            }

            assert applyCmd != null
            retryInSecs(2){
                assert releaseCmd != null
            }
        }

        def testGCOnUserdataOnVmMigrationSuccessButFailedToReleaseUserdataOnSrcHost = {
            def releaseCmd = null
            boolean releaseSuccess = false

            env.afterSimulator(FlatUserdataBackend.RELEASE_USER_DATA) { rsp, HttpEntity<String> e ->
                if (!releaseSuccess) {
                    throw new Exception("failed to release on purpose")
                }

                FlatUserdataBackend.ReleaseUserdataCmd cmd = json(e.body, FlatUserdataBackend.ReleaseUserdataCmd.class)
                releaseCmd = cmd
                return rsp
            }

            HostInventory kvm = env.inventoryByName("kvm")
            HostInventory kvm1 = env.inventoryByName("kvm1")
            VmInstanceVO vmvo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
            String srcHostUuid = vmvo.getHostUuid()
            String targetHostUuid = vmvo.getHostUuid() == kvm.uuid ? kvm1.uuid : kvm.uuid

            migrateVm {
                vmInstanceUuid = vmvo.uuid
                hostUuid = targetHostUuid
            }

            releaseSuccess = true

            retryInSecs {
                assert releaseCmd != null
                assert releaseCmd.hostUuid == srcHostUuid
            }
        }

        def testUserdataReleaseOnDestHostWhenVmMigrationFailure = {
            FlatUserdataBackend.ReleaseUserdataCmd releaseCmd = null
            env.afterSimulator(FlatUserdataBackend.RELEASE_USER_DATA) { rsp, HttpEntity<String> e ->
                FlatUserdataBackend.ReleaseUserdataCmd cmd = json(e.body, FlatUserdataBackend.ReleaseUserdataCmd.class)
                releaseCmd = cmd
                return rsp
            }

            env.simulator(KVMConstant.KVM_MIGRATE_VM_PATH) {
                throw new Exception("on purpose")
            }

            HostInventory kvm = env.inventoryByName("kvm")
            HostInventory kvm1 = env.inventoryByName("kvm1")
            VmInstanceVO vmvo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
            String targetHostUuid = vmvo.getHostUuid() == kvm.uuid ? kvm1.uuid : kvm.uuid

            expect(AssertionError.class) {
                migrateVm {
                    vmInstanceUuid = vm.uuid
                    hostUuid = targetHostUuid
                }
            }

            retryInSecs {
                assert releaseCmd != null
                assert releaseCmd.hostUuid == targetHostUuid
            }
        }

        def testGCOnUserdataOnVmMigrationFailureButFailedToReleaseUserdataOnDstHost = {
            def releaseCmd = null
            boolean releaseSuccess = false

            env.afterSimulator(FlatUserdataBackend.RELEASE_USER_DATA) { rsp, HttpEntity<String> e ->
                if (!releaseSuccess) {
                    throw new Exception("failed to release on purpose")
                }

                FlatUserdataBackend.ReleaseUserdataCmd cmd = json(e.body, FlatUserdataBackend.ReleaseUserdataCmd.class)
                releaseCmd = cmd
                return rsp
            }

            HostInventory kvm = env.inventoryByName("kvm")
            HostInventory kvm1 = env.inventoryByName("kvm1")
            VmInstanceVO vmvo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
            String targetHostUuid = vmvo.getHostUuid() == kvm.uuid ? kvm1.uuid : kvm.uuid

            env.simulator(KVMConstant.KVM_MIGRATE_VM_PATH) {
                throw new Exception("on purpose")
            }

            expect(AssertionError.class) {
                migrateVm {
                    vmInstanceUuid = vm.uuid
                    hostUuid = targetHostUuid
                }
            }

            assert releaseCmd == null

            releaseSuccess = true

            retryInSecs {
                assert releaseCmd != null
                assert releaseCmd.hostUuid == targetHostUuid
            }
        }

        testUserdataOnVmMigrationSuccess()
        testGCOnUserdataOnVmMigrationSuccessButFailedToReleaseUserdataOnSrcHost()
        testUserdataReleaseOnDestHostWhenVmMigrationFailure()
        testGCOnUserdataOnVmMigrationFailureButFailedToReleaseUserdataOnDstHost()
    }

    void testSetAndDeleteUserddataWhenRebootVm() {
        testSetUserdataSetWhenVmOperations {
            rebootVmInstance {
                uuid = vm.uuid
            }
        }

        testDeleteUserdataSetWhenVmOperations {
            rebootVmInstance {
                uuid = vm.uuid
            }
        }
    }

    void testDeleteUserdataWhenStopVm() {
        testDeleteUserdataSetWhenVmOperations {
            stopVmInstance {
                uuid = vm.uuid
            }
        }
    }

    void testSetUserdataWhenCreateVm() {
        testSetUserdataSetWhenVmOperations() {
            ImageSpec image = env.specByName("image")
            InstanceOfferingSpec offering = env.specByName("instanceOffering")

            vm = createVmInstance {
                name = "vm"
                imageUuid = image.inventory.uuid
                l3NetworkUuids = [l3.uuid]
                instanceOfferingUuid = offering.inventory.uuid
                systemTags = [VmSystemTags.USERDATA.instantiateTag([(VmSystemTags.USERDATA_TOKEN): new String(Base64.getEncoder().encode(userdata.getBytes()))])]
            }
        }
    }

    private void testDeleteUserdataSetWhenVmOperations(Closure vmOperation) {
        FlatUserdataBackend.ReleaseUserdataCmd cmd = null

        env.afterSimulator(FlatUserdataBackend.RELEASE_USER_DATA) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, FlatUserdataBackend.ReleaseUserdataCmd.class)
            return rsp
        }

        vmOperation()

        assert cmd != null
        assert cmd.namespaceName != null
        assert cmd.vmIp == vm.vmNics[0].ip
        assert cmd.bridgeName == new BridgeNameFinder().findByL3Uuid(l3.uuid)
    }

    private void testSetUserdataSetWhenVmOperations(Closure vmOperation) {
        FlatUserdataBackend.ApplyUserdataCmd cmd = null

        env.afterSimulator(FlatUserdataBackend.APPLY_USER_DATA) { rsp, HttpEntity<String> e ->
            cmd = json(e.body, FlatUserdataBackend.ApplyUserdataCmd.class)
            return rsp
        }

        vmOperation()

        assert cmd != null
        String dhcpIp = FlatNetworkSystemTags.L3_NETWORK_DHCP_IP
                .getTokenByResourceUuid(l3.uuid, FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_TOKEN)
        assert dhcpIp == cmd.userdata.dhcpServerIp
        assert new BridgeNameFinder().findByL3Uuid(l3.uuid) == cmd.userdata.bridgeName

        VmNicInventory nic = vm.vmNics[0]
        assert nic.ip == cmd.userdata.vmIp
        assert nic.netmask == cmd.userdata.netmask
        assert nic.l3NetworkUuid == cmd.userdata.l3NetworkUuid
        assert cmd.userdata.namespaceName != null
        assert UserdataGlobalProperty.HOST_PORT == cmd.userdata.port
        assert 1 == cmd.userdata.userdataList.size()
        assert userdata == cmd.userdata.userdataList.get(0)

        assert vm.uuid == cmd.userdata.metadata.vmUuid
    }

    @Override
    void clean() {
        env.delete()
    }
}
