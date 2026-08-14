package org.zstack.test.integration.storage.primary.local

import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.SimpleQuery
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.*
import org.zstack.storage.primary.local.LocalStorageHostRefVO
import org.zstack.storage.primary.local.LocalStorageHostRefVO_
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by lining on 2017/3/13.
 */
class MultiClusterDetachedLocalStorageAvailableCapacityCase extends SubCase{
    EnvSpec env

    DatabaseFacade dbf

    long volumeBitSize = SizeUnit.GIGABYTE.toByte(10)

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = env{

            diskOffering {
                name = 'diskOffering'
                diskSize = volumeBitSize
            }

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
                    name = "image1"
                    url  = "http://zstack.org/download/test.qcow2"
                    size = volumeBitSize
                }

                image {
                    size = volumeBitSize
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
                        name = "kvm"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                cluster {
                    name = "cluster2"
                    hypervisorType = "KVM"

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
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
                useRootDiskOffering("diskOffering")
                useCluster("cluster")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testPSAvailableCapacity()
        }
    }

    void testPSAvailableCapacity(){
        dbf = bean(DatabaseFacade.class)

        ClusterInventory cluster = env.inventoryByName("cluster")
        PrimaryStorageInventory ps = env.inventoryByName("local")
        HostInventory host = env.inventoryByName("kvm")
        DiskOfferingInventory diskOfferingInventory = env.inventoryByName("diskOffering")
        VmInstanceInventory vm = env.inventoryByName("vm")
        SimpleQuery<LocalStorageHostRefVO> hq = dbf.createQuery(LocalStorageHostRefVO.class)
        hq.add(LocalStorageHostRefVO_.hostUuid, SimpleQuery.Op.EQ, host.uuid)
        hq.add(LocalStorageHostRefVO_.primaryStorageUuid, SimpleQuery.Op.EQ, ps.uuid)
        LocalStorageHostRefVO localStorageHostRefVO = hq.find()

        VolumeInventory dataVolume = createDataVolume {
            name = "dataVolume"
            diskOfferingUuid = diskOfferingInventory.uuid
            primaryStorageUuid = ps.uuid
            systemTags = ["localStorage::hostUuid::${host.uuid}".toString()]
        }
        attachDataVolumeToVm {
            vmInstanceUuid = vm.uuid
            volumeUuid = dataVolume.uuid
        }

        // check PrimaryStorageCapacityVO.availableCapacity, LocalStorageHostRefVO.availableCapacity
        assert localStorageHostRefVO.availableCapacity > 0 && ps.availableCapacity > 0

        // detach ps
        detachPrimaryStorageFromCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = cluster.uuid
        }

        // check PrimaryStorageCapacityVO capacity != 0
        boolean retryResult = retryInSecs(2) {
            ps = queryPrimaryStorage {
                conditions=["uuid=${ps.uuid}".toString()]
            }[0]
            return  0 == ps.availableCapacity
        }
        assert false == retryResult
        assert 0 != ps.availableCapacity
        assert 0 != ps.availablePhysicalCapacity
        assert 0 != ps.totalCapacity
        assert 0 != ps.totalPhysicalCapacity

    }

    @Override
    void clean() {
        env.delete()
    }
}
