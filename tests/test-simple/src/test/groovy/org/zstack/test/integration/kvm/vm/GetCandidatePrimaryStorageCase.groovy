package org.zstack.test.integration.kvm.vm

import org.zstack.core.db.Q
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO_
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.GetCandidatePrimaryStoragesForCreatingVmAction
import org.zstack.sdk.GetCandidatePrimaryStoragesForCreatingVmResult
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by MaJin on 2017-08-19.
 */
class GetCandidatePrimaryStorageCase extends SubCase{
    EnvSpec env
    ImageInventory imageOnSftp, imageOnCeph
    L3NetworkInventory l3
    PrimaryStorageInventory ps
    DiskOfferingInventory disk
    ImageInventory iso

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
            }

            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(20L)
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image-on-sftp"
                    url = "http://zstack.org/download/test.qcow2"
                }

                image {
                    name = "iso"
                    url = "http://zstack.org/download/test.iso"
                    format = "iso"
                }

                image {
                    name = "vr-image"
                    url = "http://zstack.org/download/vr.qcow2"
                }
            }

            cephBackupStorage {
                name="ceph"
                totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                availableCapacity= SizeUnit.GIGABYTE.toByte(100)
                fsid ="7ff218d9-f525-435f-8a40-3618d1772a64"
                monUrls = ["root:password@localhost/?monPort=7777"]

                image {
                    name = "image-on-ceph"
                    url  = "http://zstack.org/download/image.qcow2"
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
                        totalCpu = 40
                        totalMem = SizeUnit.GIGABYTE.toByte(320)
                    }

                    attachPrimaryStorage("smp")
                    attachL2Network("l2")
                }

                cluster {
                    name = "cluster-ceph-1"
                    hypervisorType = "KVM"
                    attachPrimaryStorage("ceph-1")
                    attachL2Network("l2")
                }

                cluster {
                    name = "cluster-ceph-2"
                    hypervisorType = "KVM"
                    attachPrimaryStorage("ceph-2")
                    attachL2Network("l2")
                }

                smpPrimaryStorage {
                    name = "smp"
                    url = "/opt/smp"
                }

                cephPrimaryStorage {
                    name="ceph-1"
                    description="Test"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                    availableCapacity= SizeUnit.GIGABYTE.toByte(100)
                    fsid="7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls=["root:password@localhost/?monPort=7777"]

                }

                cephPrimaryStorage {
                    name="ceph-2"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                    availableCapacity= SizeUnit.GIGABYTE.toByte(100)
                    fsid="2ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls=["root:password@127.0.0.10/?monPort=7777"]

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
                    useImage("vr-image")
                }

                attachBackupStorage("sftp", "ceph")
            }

        }
    }

    @Override
    void test() {
        env.create {
            imageOnSftp = env.inventoryByName("image-on-sftp") as ImageInventory
            imageOnCeph = env.inventoryByName("image-on-ceph") as ImageInventory
            l3 = env.inventoryByName("l3") as L3NetworkInventory
            ps = env.inventoryByName("smp") as PrimaryStorageInventory
            disk = env.inventoryByName("diskOffering") as DiskOfferingInventory
            iso = env.inventoryByName("iso") as ImageInventory
            testGetPsForCreatingVm()
            testGetCephPsCandidate()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testGetPsForCreatingVm(){
        GetCandidatePrimaryStoragesForCreatingVmResult result = getCandidatePrimaryStoragesForCreatingVm {
            imageUuid = imageOnSftp.uuid
            l3NetworkUuids = [l3.uuid]
        } as GetCandidatePrimaryStoragesForCreatingVmResult

        assert result.rootVolumePrimaryStorages.size() == 1
        assert result.dataVolumePrimaryStorages.size() == 0

        result = getCandidatePrimaryStoragesForCreatingVm {
            imageUuid = iso.uuid
            rootDiskOfferingUuid = disk.uuid
            l3NetworkUuids = [l3.uuid]
        } as GetCandidatePrimaryStoragesForCreatingVmResult

        assert result.rootVolumePrimaryStorages.size() == 1
        assert result.dataVolumePrimaryStorages.size() == 0

        result = getCandidatePrimaryStoragesForCreatingVm {
            imageUuid = iso.uuid
            rootDiskOfferingUuid = disk.uuid
            dataDiskOfferingUuids = [disk.uuid]
            l3NetworkUuids = [l3.uuid]
        } as GetCandidatePrimaryStoragesForCreatingVmResult

        assert result.rootVolumePrimaryStorages.size() == 1
        assert result.dataVolumePrimaryStorages.size() == 1

        GetCandidatePrimaryStoragesForCreatingVmAction a = new GetCandidatePrimaryStoragesForCreatingVmAction()
        a.imageUuid = iso.uuid
        a.l3NetworkUuids = [l3.uuid]
        a.sessionId = currentEnvSpec.session.uuid
        assert a.call().error != null

        assert Q.New(PrimaryStorageCapacityVO.class).eq(PrimaryStorageCapacityVO_.uuid, ps.uuid)
                .select(PrimaryStorageCapacityVO_.availableCapacity).findValue() == ps.availableCapacity
    }

    void testGetCephPsCandidate() {
        def cephRelateBs = env.inventoryByName("ceph-1") as PrimaryStorageInventory
        def result = getCandidatePrimaryStoragesForCreatingVm {
            imageUuid = imageOnCeph.uuid
            l3NetworkUuids = [l3.uuid]
        } as GetCandidatePrimaryStoragesForCreatingVmResult

        assert result.rootVolumePrimaryStorages.size() == 1
        assert result.rootVolumePrimaryStorages.get(0).uuid == cephRelateBs.uuid
        assert result.dataVolumePrimaryStorages.size() == 0
    }
}
