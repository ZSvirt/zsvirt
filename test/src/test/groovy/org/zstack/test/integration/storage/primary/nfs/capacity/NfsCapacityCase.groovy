package org.zstack.test.integration.storage.primary.nfs.capacity

import org.zstack.header.image.ImageConstant
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.GetPrimaryStorageCapacityResult
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by AlanJager on 2017/4/21.
 */
class NfsCapacityCase extends SubCase {
    EnvSpec env
    PrimaryStorageInventory ps

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
                    name = "iso"
                    url = "http://zstack.org/download/test.iso"
                    mediaType = ImageConstant.ImageMediaType.ISO.toString()
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
                        name = "kvm"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("nfs")
                    attachL2Network("l2")
                }

                cluster {
                    name = "cluster2"
                    hypervisorType = "KVM"
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "localhost:/nfs"
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
                useHost("kvm")
            }
        }
    }

    @Override
    void test() {
        env.create {
            ps = env.inventoryByName("nfs")
            testReconnectPrimaryStorageCapacityRecalculation()
            testMultiPrimaryStorageCapacity()
        }
    }

    void testReconnectPrimaryStorageCapacityRecalculation() {
        L3NetworkInventory l3 = env.inventoryByName("l3")
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("iso")
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering")

        GetPrimaryStorageCapacityResult beforeCapacity = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }

        createVmInstance {
            name = "test"
            l3NetworkUuids = [l3.uuid]
            imageUuid = image.uuid
            rootDiskOfferingUuid = diskOffering.uuid
            instanceOfferingUuid = instanceOffering.uuid
        }

        GetPrimaryStorageCapacityResult afterCreateVmInstance = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }
        assert beforeCapacity.availableCapacity > afterCreateVmInstance.availableCapacity

        reconnectPrimaryStorage {
            uuid = ps.uuid
        }

        GetPrimaryStorageCapacityResult afterReconnectPS = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }
        assert afterReconnectPS.availableCapacity == afterCreateVmInstance.availableCapacity
        assert afterReconnectPS.totalCapacity == afterCreateVmInstance.totalCapacity
        assert beforeCapacity.totalCapacity == afterReconnectPS.totalCapacity
    }

    void testMultiPrimaryStorageCapacity(){
        def cluster1 = env.inventoryByName("cluster")
        def cluster2 = env.inventoryByName("cluster2")

        assert getPrimaryStorageCapacity {
            clusterUuids = [cluster2.uuid]
        }.availableCapacity == 0

        long before = getPrimaryStorageCapacity {
            clusterUuids = [cluster1.uuid, cluster2.uuid]
        }.availableCapacity

        attachPrimaryStorageToCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = cluster2.uuid
        }

        long after = getPrimaryStorageCapacity {
            clusterUuids = [cluster1.uuid, cluster2.uuid]
        }.availableCapacity

        assert before == after
    }
}
