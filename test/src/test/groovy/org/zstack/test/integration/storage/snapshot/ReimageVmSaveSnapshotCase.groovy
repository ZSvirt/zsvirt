package org.zstack.test.integration.storage.snapshot

import org.zstack.core.db.Q
import org.zstack.header.core.trash.InstallPathRecycleVO
import org.zstack.header.core.trash.InstallPathRecycleVO_;
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by liangbo.zhou on 17-6-24.
 */
class ReimageVmSaveSnapshotCase extends SubCase {

    EnvSpec env

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
            }

            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(20)
            }

            cephBackupStorage {
                name="ceph-bk"
                description="Test"
                totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                availableCapacity= SizeUnit.GIGABYTE.toByte(100)
                url = "/bk"
                fsid ="7ff218d9-f525-435f-8a40-3618d1772a64"
                monUrls = ["root:password@localhost/?monPort=7777"]

                image {
                    name = "test-iso"
                    url  = "http://zstack.org/download/test.iso"
                    format = "iso"
                }
                image {
                    name = "image"
                    url  = "http://zstack.org/download/image.qcow2"
                }
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image1"
                    url  = "http://zstack.org/download/test.qcow2"
                }

                image {
                    name = "test-iso"
                    url  = "http://zstack.org/download/test.iso"
                    format = "iso"
                }

                image {
                    name = "vr"
                    url  = "http://zstack.org/download/vr.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "local-cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm1"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                cluster {
                    name = "ceph-cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("ceph")
                    attachL2Network("l2")
                }

                cluster {
                    name = "nfs-cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm3"
                        managementIp = "127.0.0.3"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }

                cluster {
                    name = "smp-cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm4"
                        managementIp = "127.0.0.4"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("smp")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "localhost:/nfs"
                }

                cephPrimaryStorage {
                    name="ceph"
                    description="Test"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                    availableCapacity= SizeUnit.GIGABYTE.toByte(100)
                    url="ceph://pri"
                    fsid="7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls=["root:password@localhost/?monPort=7777"]
                }

                smpPrimaryStorage {
                    name = "smp"
                    url = "/test"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        service {
                            provider = VirtualRouterConstant.PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(), NetworkServiceType.DNS.toString()]
                        }

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }

                    l3Network {
                        name = "l3_2"

                        service {
                            provider = VirtualRouterConstant.PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(), NetworkServiceType.DNS.toString()]
                        }

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "192.168.200.10"
                            endIp = "192.168.200.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.200.1"
                        }
                    }
                    l3Network {
                        name = "pubL3"

                        ip {
                            startIp = "12.16.10.10"
                            endIp = "12.16.10.100"
                            netmask = "255.255.255.0"
                            gateway = "12.16.10.1"
                        }
                    }
                }

                virtualRouterOffering {
                    name = "vr"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3")
                    usePublicL3Network("pubL3")
                    useImage("vr")
                }

                attachBackupStorage("sftp")
                attachBackupStorage("ceph-bk")
            }

            vm {
                name = "local-vm"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
                useRootDiskOffering("diskOffering")
                useHost("kvm1")
            }

            vm {
                name = "ceph-vm"
                useInstanceOffering("instanceOffering")
                useImage("image")
                useL3Networks("l3")
                useHost("kvm2")
            }

            vm {
                name = "smp-vm"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
                useRootDiskOffering("diskOffering")
                useHost("kvm4")
            }

            vm {
                name = "nfs-vm"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
                useRootDiskOffering("diskOffering")
                useHost("kvm3")
            }
        }

    }

    @Override
    void test() {
        
        env.create {
            testReimageVmfailCase()
            testReimageVmSaveSnapshotLocal()
            testReimageVmSaveSnapshotCeph()
            testReimageVmSaveSnapshotSmp()
            testReimageVmSaveSnapshotNfs()
        }

    }

    void testReimageVmfailCase() {
        def offering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        def diskOffer = env.inventoryByName("diskOffering") as DiskOfferingInventory
        def iso = env.inventoryByName("test-iso") as ImageInventory
        def l3 = env.inventoryByName("l3") as L3NetworkInventory

        def vm = createVmInstance {
            name = "vm-iso"
            imageUuid = iso.uuid
            l3NetworkUuids = [l3.uuid]
            instanceOfferingUuid = offering.uuid
            rootDiskOfferingUuid = diskOffer.uuid
        } as VmInstanceInventory

        //fail to reinit image with running status
        expect(AssertionError.class) {
            reimageVmInstance {
                vmInstanceUuid = vm.uuid
            }
        }

        stopVmInstance {
            uuid = vm.uuid
        }

        // iso fail
        expect(AssertionError.class) {
            reimageVmInstance {
                vmInstanceUuid = vm.uuid
            }
        }

        destroyVmInstance {
            uuid = vm.uuid
        }
    }
    void testReimageVmSaveSnapshotLocal(){
        def vm = env.inventoryByName("local-vm") as VmInstanceInventory
        stopVmInstance {
            uuid = vm.uuid
        }

        reimageVmInstance {
            vmInstanceUuid = vm.uuid
        }
         assert Q.New(InstallPathRecycleVO.class)
                .eq(InstallPathRecycleVO_.resourceUuid, vm.getRootVolumeUuid()).isExists()
    }

    void testReimageVmSaveSnapshotCeph(){
        def vm = env.inventoryByName("ceph-vm") as VmInstanceInventory
        stopVmInstance {
            uuid = vm.uuid
        }

        reimageVmInstance {
            vmInstanceUuid = vm.uuid
        }
        assert Q.New(InstallPathRecycleVO.class)
                .eq(InstallPathRecycleVO_.resourceUuid, vm.getRootVolumeUuid()).isExists()
    }

    void testReimageVmSaveSnapshotSmp(){
        def vm = env.inventoryByName("smp-vm") as VmInstanceInventory
        stopVmInstance {
            uuid = vm.uuid
        }

        reimageVmInstance {
            vmInstanceUuid = vm.uuid
        }
        assert Q.New(InstallPathRecycleVO.class)
                .eq(InstallPathRecycleVO_.resourceUuid, vm.getRootVolumeUuid()).isExists()
    }

    void testReimageVmSaveSnapshotNfs(){
        def vm = env.inventoryByName("nfs-vm") as VmInstanceInventory
        stopVmInstance {
            uuid = vm.uuid
        }

        reimageVmInstance {
            vmInstanceUuid = vm.uuid
        }
        assert Q.New(InstallPathRecycleVO.class)
                .eq(InstallPathRecycleVO_.resourceUuid, vm.getRootVolumeUuid()).isExists()
    }
    @Override
    void clean() {
        env.delete()
    }
}
