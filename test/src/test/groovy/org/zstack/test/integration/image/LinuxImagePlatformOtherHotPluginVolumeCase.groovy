package org.zstack.test.integration.image

import org.springframework.http.HttpEntity
import org.zstack.header.image.ImagePlatform
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.core.db.UpdateQuery
import org.zstack.header.volume.VolumeStatus
import org.zstack.header.volume.VolumeVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by heathhose on 17-5-22.
 */
class LinuxImagePlatformOtherHotPluginVolumeCase extends SubCase{
    def DOC = """
The vm doesn't support to attach vm, which use image-1 that platform type is other
"""

    EnvSpec env
    VolumeInventory volume
    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image-1"
                    url = "http://zstack.org/download/test.qcow2"
                    platform = ImagePlatform.Other.toString()
                }

                image {
                    name = "image-2"
                    url = "http://zstack.org/download/test.qcow2"
                }

                image   {
                    name = "vr"
                    url = "http://zstack.org/download/vr.qcow2"
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

                    l3Network {
                        name = "pubL3"

                        ip {
                            startIp = "11.168.100.10"
                            endIp = "11.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.100.1"
                        }
                    }
                }

                attachBackupStorage("sftp")
            }

            vm {
                cpu = 4
                memoryGB(8)
                name = "vm-other"
                useImage("image-1")
                useL3Networks("l3")
            }

            vm {
                cpu = 4
                memoryGB(8)
                name = "vm-2"
                useImage("image-2")
                useL3Networks("l3")
            }

        }
    }

    @Override
    void test() {
        env.create {
            testGetCandidateVmForAttaching()
            testAttachVolumeToVm()
            testAttachVolumeToVmImageOther()
        }
    }

    void testGetCandidateVmForAttaching(){
        def vm = env.inventoryByName("vm-other") as VmInstanceInventory
        def vm2  = env.inventoryByName("vm-2") as VmInstanceInventory
        def volume = createDataVolume {
            name = "test-1"
            diskSize = SizeUnit.GIGABYTE.toByte(10)
        } as VolumeInventory

        //vms are runing
        def vms = getDataVolumeAttachableVm {
            volumeUuid = volume.uuid
        } as List<VmInstanceInventory>
        assert vms.size() == 1
        assert vms.get(0).uuid == vm2.uuid

        //stop vm-other and retry
        stopVmInstance {
           uuid = vm.uuid
        }
        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).getState() == VmInstanceState.Stopped
        vms = getDataVolumeAttachableVm {
            volumeUuid = volume.uuid
        } as List<VmInstanceInventory>
        assert vms.size() == 2

        startVmInstance {
            uuid = vm.uuid
        }
        assert dbFindByUuid(vm.uuid, VmInstanceVO.class).getState() == VmInstanceState.Running
    }

    void testAttachVolumeToVm(){
        def vm = env.inventoryByName("vm-2") as VmInstanceInventory
        def volume = createDataVolume {
            name = "test-2" 
            diskSize = SizeUnit.GIGABYTE.toByte(10)
        } as VolumeInventory
        
        KVMAgentCommands.AttachDataVolumeCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_ATTACH_VOLUME){rsp, HttpEntity<String> entity ->
            cmd = json(entity.getBody(), KVMAgentCommands.AttachDataVolumeCmd.class)
            return rsp
        }
        attachDataVolumeToVm {
            volumeUuid = volume.uuid
            vmInstanceUuid = vm.uuid
        }

        assert dbFindByUuid(volume.uuid,VolumeVO.class).status == VolumeStatus.Ready
        assert cmd.vmUuid == vm.uuid
        env.cleanAfterSimulatorHandlers()
    }
    
    void testAttachVolumeToVmImageOther(){
        def vm = env.inventoryByName("vm-other") as VmInstanceInventory
        def volume = createDataVolume {
            name = "test-3"
            diskSize = SizeUnit.GIGABYTE.toByte(10)
        } as VolumeInventory

        UpdateQuery.New(VmInstanceVO.class)
                .set(VmInstanceVO_.platform, "Other").
                eq(VmInstanceVO_.uuid, vm.uuid)
                .update()

        //vm is running and attach volume
        expect(AssertionError.class){
            attachDataVolumeToVm {
                volumeUuid = volume.uuid
                vmInstanceUuid = vm.uuid
            }
        }
        assert dbFindByUuid(volume.uuid,VolumeVO.class).status == VolumeStatus.NotInstantiated

        UpdateQuery.New(VmInstanceVO.class)
                .set(VmInstanceVO_.platform, "Linux").
                eq(VmInstanceVO_.uuid, vm.uuid)
                .update()

        attachDataVolumeToVm {
            volumeUuid = volume.uuid
            vmInstanceUuid = vm.uuid
        }

        detachDataVolumeFromVm {
            uuid = volume.uuid
        }

        UpdateQuery.New(VmInstanceVO.class)
                .set(VmInstanceVO_.platform, "Other").
                eq(VmInstanceVO_.uuid, vm.uuid)
                .update()

        //stop vm and retry
        //vm is stop and AttachDataVolumeCmd is not called
        stopVmInstance {
            uuid = vm.uuid
        }
        attachDataVolumeToVm {
            volumeUuid = volume.uuid
            vmInstanceUuid = vm.uuid
        }
        assert dbFindByUuid(volume.uuid,VolumeVO.class).status == VolumeStatus.Ready

    }
    @Override
    void clean() {
        env.delete()
    }
}
