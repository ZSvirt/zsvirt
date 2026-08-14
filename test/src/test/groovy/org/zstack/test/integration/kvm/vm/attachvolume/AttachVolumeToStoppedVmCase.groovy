package org.zstack.test.integration.kvm.vm.attachvolume

import org.zstack.header.vm.VmInstanceVO
import org.zstack.kvm.KVMGlobalConfig
import org.zstack.sdk.AttachDataVolumeToVmAction
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.UpdateGlobalConfigAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by lining on 2017/7/04.
 */
class AttachVolumeToStoppedVmCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
        spring {
            ceph()
        }
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

            zone{
                name = "zone"
                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                        usedMem = 1000
                        totalCpu = 10
                    }

                    attachPrimaryStorage("ps")
                    attachL2Network("l2")
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

                cephPrimaryStorage {
                    name="ps"
                    description="Test"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                    availableCapacity= SizeUnit.GIGABYTE.toByte(100)
                    url="ceph://pri"
                    fsid="7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls=["root:password@localhost/?monPort=7777"]

                }
                attachBackupStorage("bs")
            }

            cephBackupStorage {
                name="bs"
                description="Test"
                totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                availableCapacity= SizeUnit.GIGABYTE.toByte(100)
                url = "/bk"
                fsid ="7ff218d9-f525-435f-8a40-3618d1772a64"
                monUrls = ["root:password@localhost/?monPort=7777"]

                image {
                    name = "image"
                    url  = "http://zstack.org/download/image.qcow2"
                }
            }

            vm {
                name = "vm"
                useCluster("cluster")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
                useImage("image")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testAttachWhenVmLastHostUuidIsNotNull()
            testAttachWhenVmLastHostUuidIsNull()
            testAttachVolumeGlobalConfig()
        }
    }

    void testAttachWhenVmLastHostUuidIsNotNull() {
        VmInstanceInventory vm = env.inventoryByName("vm")
        HostInventory host = env.inventoryByName("kvm")
        DiskOfferingInventory diskOfferingInventory = env.inventoryByName("diskOffering")

        stopVmInstance {
            uuid = vm.uuid
        }
        VmInstanceVO vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vo.getHostUuid() == null
        assert vo.getLastHostUuid() == vm.getHostUuid()

        VolumeInventory vol = createDataVolume {
            name = "data"
            diskOfferingUuid = diskOfferingInventory.uuid
        }

        attachDataVolumeToVm {
            vmInstanceUuid = vm.uuid
            volumeUuid = vol.uuid
        }

        detachDataVolumeFromVm {
            uuid = vol.uuid
        }

        vol = queryVolume {
            conditions = ["uuid=${vol.uuid}"]
        }[0] as VolumeInventory

        assert vol.lastDetachDate != null
        assert vol.lastVmInstanceUuid == vm.uuid
    }

    void testAttachWhenVmLastHostUuidIsNull() {
        VmInstanceInventory vm = env.inventoryByName("vm")
        HostInventory host = env.inventoryByName("kvm")
        DiskOfferingInventory diskOfferingInventory = env.inventoryByName("diskOffering")

        deleteHost {
            uuid = host.uuid
        }
        retryInSecs(3,1) {
            VmInstanceVO vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
            assert vo.getHostUuid() == null
            assert vo.getLastHostUuid() == null
        }

        VolumeInventory vol = createDataVolume {
            name = "data"
            diskOfferingUuid = diskOfferingInventory.uuid
        }
        
        AttachDataVolumeToVmAction action = new AttachDataVolumeToVmAction(
                vmInstanceUuid: vm.uuid,
                volumeUuid: vol.uuid,
                sessionId: currentEnvSpec.session.uuid
        )

        // can't checkout concrete content of error message
        // assert action.call().error.details.indexOf("not find the vm's host") > 0
        assert action.call().error != null
    }

    void testAttachVolumeGlobalConfig(){
        updateGlobalConfig {
            category = KVMGlobalConfig.CATEGORY
            name = KVMGlobalConfig.MAX_DATA_VOLUME_NUM.name
            value = "0"
        }

        updateGlobalConfig {
            category = KVMGlobalConfig.CATEGORY
            name = KVMGlobalConfig.MAX_DATA_VOLUME_NUM.name
            value = "24"
        }

        def a = new UpdateGlobalConfigAction()
        a.category = KVMGlobalConfig.CATEGORY
        a.name = KVMGlobalConfig.MAX_DATA_VOLUME_NUM.name
        a.value = "25"
        a.sessionId = adminSession()

        assert a.call().error != null
    }
}
