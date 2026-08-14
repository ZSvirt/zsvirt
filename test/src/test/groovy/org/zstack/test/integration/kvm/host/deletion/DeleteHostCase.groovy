package org.zstack.test.integration.kvm.host.deletion

import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.volume.VolumeStatus
import org.zstack.header.volume.VolumeVO
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by lining on 2017/3/12.
 */

/**
 * 1. create a 1 vm(vm1) on the host1
 * 2. attach data volume to vm1 which is on the host1
 * 3. create 1 vm(vm2) on the host2 and attach 1 data volume to the vm2
 * 4. delete the host1
 * <p>
 * confirm vm  are deleted, data volumes still exist
 * confirm the vm2 and its data volumes are not effected by the host1 deletion
 */
// base on TestMevoco19
class DeleteHostCase extends SubCase{

    EnvSpec env

    DatabaseFacade dbf

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
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
                    name = "image1"
                    url  = "http://zstack.org/download/test.qcow2"
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
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
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
            }

            vm {
                name = "vm"
                memorySize = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
                useImage("image1")
                useL3Networks("l3")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testDataVolumeStatusWhenDeleteHost()
            testForceDeleteHost()
        }
    }


    void testDataVolumeStatusWhenDeleteHost() {
        dbf = bean(DatabaseFacade.class)

        HostInventory host1 = env.inventoryByName("kvm1")
        HostInventory host2 = env.inventoryByName("kvm2")
        VmInstanceInventory vm = env.inventoryByName("vm")

        // create disk on host1
        VolumeInventory vol = createDataVolume {
            name = "data1"
            diskSize = SizeUnit.GIGABYTE.toByte(10)
        }
        vol = attachDataVolumeToVm {
            vmInstanceUuid = vm.uuid
            volumeUuid = vol.uuid
        }
        assert null != vol

        // create vm on host2
        VmInstanceInventory newVm = createVmInstance {
            name = "newVm"
            memorySize = SizeUnit.GIGABYTE.toByte(8)
            cpuNum = 4
            imageUuid = vm.imageUuid
            l3NetworkUuids = [vm.defaultL3NetworkUuid]
            hostUuid = (host1.uuid == vm.hostUuid ? host2.uuid : host1.uuid)
        }
        VolumeInventory newVol = createDataVolume {
            name = "data2"
            diskSize = SizeUnit.GIGABYTE.toByte(10)
            systemTags = ["localStorage::hostUuid::${newVm.hostUuid}".toString()]
        }
        attachDataVolumeToVm {
            vmInstanceUuid = newVm.uuid
            volumeUuid = newVol.uuid
        }


        // delete host1: expected vm and vm.dataVolume also deleted, but newVm and newVm.volume still exist
        deleteHost {
            uuid = vm.hostUuid
        }

        retryInSecs(4){
            VolumeVO volumeVO = dbFindByUuid(vol.uuid, VolumeVO.class)
            assert null == volumeVO && null == dbFindByUuid(vm.uuid, VmInstanceVO.class)
        }

        newVm = queryVmInstance {
            conditions=["uuid=${newVm.uuid}".toString()]
        }[0]
        assert newVm.state == VmInstanceState.Running.name()
        assert 2 == newVm.allVolumes.size()
        assert newVm.allVolumes[0].status == VolumeStatus.Ready.name()
        assert newVm.allVolumes[1].status == VolumeStatus.Ready.name()
    }

    void testForceDeleteHost() {
        String hostUuid = Q.New(HostVO.class)
                .select(HostVO_.uuid)
                .findValue()

        deleteHost {
            uuid = hostUuid
            deleteMode = "Enforcing"
        }
        assert !Q.New(HostVO.class)
                .eq(HostVO_.uuid, hostUuid)
                .isExists()
    }

    @Override
    void clean() {
        env.delete()
    }
}
