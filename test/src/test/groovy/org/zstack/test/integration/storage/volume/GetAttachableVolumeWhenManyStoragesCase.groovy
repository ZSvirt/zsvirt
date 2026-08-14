package org.zstack.test.integration.storage.volume

import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * @Author: fubang
 * @Date: 2018/6/11
 */
class GetAttachableVolumeWhenManyStoragesCase extends SubCase {
    EnvSpec env
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
                        name = "kvm-1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    kvm {
                        name = "kvm-2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("nfs")
                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    availableCapacity = SizeUnit.GIGABYTE.toByte(100)
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                    url = "127.0.0.1:/nfs"
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
                            types = [NetworkServiceType.DHCP.toString()]
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

            volume {
                name = "vo1"
                useDiskOffering("diskOffering")
                usePrimaryStorage("local")
                useHost("kvm-1")
            }

            volume {
                name = "vo2"
                useDiskOffering("diskOffering")
                usePrimaryStorage("local")
                useHost("kvm-2")
            }

            volume {
                name = "vo3"
                useDiskOffering("diskOffering")
                usePrimaryStorage("local")
                useHost("kvm-1")
            }

            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
                useCluster("cluster")
                useHost("kvm-1")
                usePrimaryStorage("nfs")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testGetAttachableVolumeWhenVmRunning()

            testGetAttachableVolumeWhenVmStop()
        }
    }

    void testGetAttachableVolumeWhenVmRunning(){
        VmInstanceInventory inventory = env.inventoryByName("vm") as VmInstanceInventory
        String volume1Uuid = env.inventoryByName("vo1").uuid
        String volume3Uuid = env.inventoryByName("vo3").uuid

        // origin condition
        List volumes = getVmAttachableDataVolume {
            vmInstanceUuid = inventory.uuid
        }
        List volumeUuids = volumes.collect { it.uuid }

        assert volumes.size() == 2
        assert volumeUuids.contains(volume1Uuid)
        assert volumeUuids.contains(volume3Uuid)

        // attache volume condition
        attachDataVolumeToVm {
            volumeUuid = volume3Uuid
            vmInstanceUuid = inventory.uuid
        }

        volumes = getVmAttachableDataVolume {
            vmInstanceUuid = inventory.uuid
        }
        volumeUuids = volumes.collect { it.uuid }
        assert volumes.size() == 1
        assert volumeUuids.contains(volume1Uuid)

        // detach volume condition
        detachDataVolumeFromVm {
            uuid = volume3Uuid
            vmUuid = inventory.uuid
        }

        volumes = getVmAttachableDataVolume {
            vmInstanceUuid = inventory.uuid
        }
        volumeUuids = volumes.collect { it.uuid }
        assert volumes.size() == 2
        assert volumeUuids.contains(volume1Uuid)
        assert volumeUuids.contains(volume3Uuid)
    }

    void testGetAttachableVolumeWhenVmStop(){
        VmInstanceInventory inventory = env.inventoryByName("vm") as VmInstanceInventory
        String volume1Uuid = env.inventoryByName("vo1").uuid
        String volume2Uuid = env.inventoryByName("vo2").uuid
        String volume3Uuid = env.inventoryByName("vo3").uuid

        stopVmInstance {
            uuid = inventory.uuid
        }

        // origin condition
        List volumes = getVmAttachableDataVolume {
            vmInstanceUuid = inventory.uuid
        }
        List volumeUuids = volumes.collect { it.uuid }

        assert volumes.size() == 3
        assert volumeUuids.contains(volume1Uuid)
        assert volumeUuids.contains(volume2Uuid)
        assert volumeUuids.contains(volume3Uuid)

        // attache volume condition
        attachDataVolumeToVm {
            volumeUuid = volume3Uuid
            vmInstanceUuid = inventory.uuid
        }

        volumes = getVmAttachableDataVolume {
            vmInstanceUuid = inventory.uuid
        }
        volumeUuids = volumes.collect { it.uuid }
        assert volumes.size() == 1
        assert volumeUuids.contains(volume1Uuid)

        // detach volume condition
        detachDataVolumeFromVm {
            uuid = volume3Uuid
            vmUuid = inventory.uuid
        }

        volumes = getVmAttachableDataVolume {
            vmInstanceUuid = inventory.uuid
        }
        volumeUuids = volumes.collect { it.uuid }
        assert volumes.size() == 3
        assert volumeUuids.contains(volume1Uuid)
        assert volumeUuids.contains(volume2Uuid)
        assert volumeUuids.contains(volume3Uuid)
    }
}
