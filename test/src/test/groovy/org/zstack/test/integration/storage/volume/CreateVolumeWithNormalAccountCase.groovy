package org.zstack.test.integration.storage.volume

import org.zstack.compute.vm.VmQuotaConstant
import org.zstack.core.db.Q
import org.zstack.header.identity.AccountConstant
import org.zstack.header.image.ImageConstant
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.header.volume.VolumeType
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.SessionInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

class CreateVolumeWithNormalAccountCase extends SubCase{

    EnvSpec env
    AccountInventory accountInventory
    ImageInventory rootImageInventory
    VmInstanceInventory vm
    L3NetworkInventory l3
    DiskOfferingInventory diskOfferingInventory
    InstanceOfferingInventory instanceOffering
    SessionInventory adminSession
    SessionInventory normalSession

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
        env = makeEnv {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(2)
                cpu = 1
            }

            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(5)
            }

            diskOffering {
                name = "diskOffering2"
                diskSize = SizeUnit.GIGABYTE.toByte(50)
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image-root-volume"
                    url  = "http://zstack.org/download/test.qcow2"
                }

                image {
                    name = "image-data-volume"
                    mediaType = ImageConstant.ImageMediaType.DataVolumeTemplate
                    url  = "http://zstack.org/download/test-volume.qcow2"
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

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"
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

            vm {
                name = "test-vm"
                useInstanceOffering("instanceOffering")
                useImage("image-root-volume")
                useL3Networks("l3")
                useRootDiskOffering("diskOffering")
                useHost("kvm")
            }
        }
    }

    @Override
    void test() {
        env.create {
            /*
            * 1. create normal account and set volume quota value
            * 2. create volume over quota
            * */
            prepare()
            testRootVolumeOverQuota()
            testDataVolumeOverQuota()
        }
    }

    void prepare() {

        vm = env.inventoryByName("test-vm") as VmInstanceInventory
        rootImageInventory = env.inventoryByName("image-root-volume") as ImageInventory
        instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        l3 = env.inventoryByName("l3") as L3NetworkInventory
        diskOfferingInventory = env.inventoryByName("diskOffering") as DiskOfferingInventory

        accountInventory = createAccount {
            name = "normal"
            password = "password"
        } as AccountInventory

        attachPredefineRoles(accountInventory.uuid, "image", "configuration", "networks", "vm", "volume", "snapshot")

        normalSession = logInByAccount {
            accountName = "normal"
            password = "password"
        } as SessionInventory

        adminSession = logInByAccount {
            accountName = AccountConstant.INITIAL_SYSTEM_ADMIN_NAME
            password = AccountConstant.INITIAL_SYSTEM_ADMIN_PASSWORD
        } as SessionInventory

        updateQuota {
            identityUuid = accountInventory.uuid
            name = VmQuotaConstant.VM_TOTAL_NUM
            value = 3
            sessionId = adminSession.uuid
        }

        updateQuota {
            identityUuid = accountInventory.uuid
            name = VmQuotaConstant.DATA_VOLUME_NUM
            value = 2
            sessionId = adminSession.uuid
        }

        updateQuota {
            identityUuid = accountInventory.uuid
            name = VmQuotaConstant.VOLUME_SIZE
            value = SizeUnit.GIGABYTE.toByte(20)
            sessionId = adminSession.uuid
        }
    }

    void testRootVolumeOverQuota() {

        def images = queryImage {
            sessionId = normalSession.uuid
        } as List<ImageInventory>

        def instanceOfferings = queryInstanceOffering {
            sessionId = normalSession.uuid
        } as List<InstanceOfferingInventory>

        def diskOfferings = queryDiskOffering {
            sessionId = normalSession.uuid
        } as List<DiskOfferingInventory>

        def l3NetWorks = queryL3Network {
            sessionId = normalSession.uuid
        } as List<L3NetworkInventory>

        assert images.size() == 0
        assert l3NetWorks.size() == 0
        assert diskOfferings.size() == 0
        assert instanceOfferings.size() == 0

        shareResource {
            resourceUuids = [diskOfferingInventory.uuid, l3.uuid, rootImageInventory.uuid, instanceOffering.uuid]
            toPublic = true
            sessionId = adminSession.uuid
        }


        diskOfferings = queryDiskOffering {} as List<DiskOfferingInventory>

        assert diskOfferings.size() == 2

        for (int i = 1; i < 4; i++) {
            createVmInstance {
                name = String.format("test-vm-%s", i.toString())
                imageUuid = rootImageInventory.uuid
                l3NetworkUuids = [l3.uuid]
                instanceOfferingUuid = instanceOffering.uuid
                sessionId = normalSession.uuid
            } as VmInstanceInventory
        }

        /* create fourth vm will fail */
        expect(AssertionError.class) {
            createVmInstance {
                name = "test-vm-4"
                instanceOfferingUuid = instanceOffering.uuid
                imageUuid = rootImageInventory.uuid
                l3NetworkUuids = [l3.uuid]
                sessionId = normalSession.uuid
            }
        }
    }

    void testDataVolumeOverQuota() {

        def vmUuid = Q.New(VmInstanceVO.class).select(VmInstanceVO_.uuid).eq(VmInstanceVO_.name, "test-vm-1").findValue()

        for (int i = 1 ; i < 3 ; i++) {
            def volume = createDataVolume {
                name = String.format("test-data-%s", i.toString())
                diskOfferingUuid = diskOfferingInventory.uuid
                sessionId = normalSession.uuid
            } as VolumeInventory

            attachDataVolumeToVm {
                volumeUuid = volume.uuid
                vmInstanceUuid = vmUuid
                sessionId = normalSession.uuid
            }
        }

        /* create third data volume will fail*/
        expect(AssertionError.class) {
            createDataVolume {
                name = String.format("test-data-3")
                diskOfferingUuid = diskOfferingInventory.uuid
                sessionId = normalSession.uuid
            }
        }

        def diskOfferingInventory2 = env.inventoryByName("diskOffering2") as DiskOfferingInventory

        def deleteVolumeUuid = Q.New(VolumeVO.class).select(VolumeVO_.uuid)
                                    .eq(VolumeVO_.vmInstanceUuid, vmUuid)
                                    .eq(VolumeVO_.type, VolumeType.Data)
                                    .limit(1)
                                    .findValue()

        deleteDataVolume {
            uuid = deleteVolumeUuid
            sessionId = normalSession.uuid
        }

        expect(AssertionError.class) {
            createDataVolume {
                name = String.format("test-data-3")
                diskOfferingUuid = diskOfferingInventory2.uuid
                sessionId = normalSession.uuid
            }
        }
    }
}
