package org.zstack.test.integration.storage.primary

import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.image.ImageConstant
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO_
import org.zstack.header.vm.DiskAO
import org.zstack.header.volume.InstantiateRootVolumeMsg
import org.zstack.header.volume.InstantiateVolumeMsg
import org.zstack.header.volume.VolumeProtocol
import org.zstack.header.volume.VolumeType
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.storage.primary.nfs.NfsPrimaryToSftpBackupKVMBackend

class EncryptedVolumePrimaryStorageAllocationCase extends SubCase {
    EnvSpec env
    PrimaryStorageInventory zhps
    PrimaryStorageInventory supportedPs

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
            instanceOffering {
                name = "instance-offering"
                memory = SizeUnit.GIGABYTE.toByte(2)
                cpu = 2
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "data-volume-template"
                    url = "http://zstack.org/download/data.qcow2"
                    mediaType = ImageConstant.ImageMediaType.DataVolumeTemplate.toString()
                }

                image {
                    name = "root-volume-template"
                    url = "http://zstack.org/download/root.qcow2"
                    mediaType = ImageConstant.ImageMediaType.RootVolumeTemplate.toString()
                }
            }

            zone {
                name = "zone"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("zhps")
                    attachPrimaryStorage("supported-ps")
                    attachL2Network("l2")
                }

                externalPrimaryStorage {
                    name = "zhps"
                    identity = "zbs"
                    defaultOutputProtocol = "CBD"
                    config = '{"mdsUrls":["root:password@127.0.1.1","root:password@127.0.1.2","root:password@127.0.1.3"],"logicalPoolName":"lpool1"}'
                    url = "fake-url"
                }

                nfsPrimaryStorage {
                    name = "supported-ps"
                    url = "127.0.0.1:/nfs"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.20"
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
            zhps = env.inventoryByName("zhps") as PrimaryStorageInventory
            supportedPs = env.inventoryByName("supported-ps") as PrimaryStorageInventory
            configureZhpsAllocatorMetadata()
            testFilterZhpsForEncryptedRootImageAutoAllocation()
            testFilterZhpsForEncryptedDataDiskAutoAllocation()
            testKeepPlainRootExplicitZhpsWhenEncryptedDataDiskAutoAllocation()
            testFilterZhpsForEncryptedTemplateDataVolumeAutoAllocation()
        }
    }

    private void configureZhpsAllocatorMetadata() {
        // Expon tests require physical storage, so reuse the simulated external PS and change allocator-visible metadata only.
        SQL.New(ExternalPrimaryStorageVO.class)
                .eq(ExternalPrimaryStorageVO_.uuid, zhps.uuid)
                .set(ExternalPrimaryStorageVO_.identity, "expon")
                .set(ExternalPrimaryStorageVO_.defaultProtocol, VolumeProtocol.Vhost.name())
                .update()

        String identity = Q.New(ExternalPrimaryStorageVO.class)
                .select(ExternalPrimaryStorageVO_.identity)
                .eq(ExternalPrimaryStorageVO_.uuid, zhps.uuid)
                .findValue()
        assert identity == "expon" : "ZHPS fixture identity mismatch: expected=expon actual=${identity}"
        String protocol = Q.New(ExternalPrimaryStorageVO.class)
                .select(ExternalPrimaryStorageVO_.defaultProtocol)
                .eq(ExternalPrimaryStorageVO_.uuid, zhps.uuid)
                .findValue()
        assert protocol == VolumeProtocol.Vhost.name() : \
                "ZHPS fixture protocol mismatch: expected=${VolumeProtocol.Vhost.name()} actual=${protocol}"
    }

    void testFilterZhpsForEncryptedRootImageAutoAllocation() {
        ImageInventory image = env.inventoryByName("root-volume-template") as ImageInventory
        String allocatedRootPsUuid = null

        def cleanup = notifyWhenReceivedMessage(InstantiateVolumeMsg.class) { InstantiateVolumeMsg msg ->
            if (msg instanceof InstantiateRootVolumeMsg) {
                allocatedRootPsUuid = msg.primaryStorageUuid
            }
        }

        try {
            DiskAO rootDisk = DiskAO.rootDisk()
            rootDisk.encrypted = true
            expectApiFailure({
                createVmInstance {
                    name = "vm-with-encrypted-root-image"
                    instanceOfferingUuid = (env.inventoryByName("instance-offering") as InstanceOfferingInventory).uuid
                    imageUuid = image.uuid
                    zoneUuid = supportedPs.zoneUuid
                    clusterUuid = (env.inventoryByName("cluster") as ClusterInventory).uuid
                    hostUuid = (env.inventoryByName("kvm") as HostInventory).uuid
                    l3NetworkUuids = [(env.inventoryByName("l3") as L3NetworkInventory).uuid]
                    defaultL3NetworkUuid = (env.inventoryByName("l3") as L3NetworkInventory).uuid
                    delegate.diskAOs = [rootDisk]
                }
            }) {
                assert details.contains("no key provider") : \
                        "Encrypted root image VM creation should reach encryption after PS allocation: details=${details}"
            }
        } finally {
            cleanup()
        }

        assert allocatedRootPsUuid == supportedPs.uuid : \
                "encrypted root image auto allocation selected unsupported ZHPS: expected=${supportedPs.uuid} actual=${allocatedRootPsUuid}"
    }

    void testFilterZhpsForEncryptedDataDiskAutoAllocation() {
        String allocatedDataPsUuid = null

        def cleanup = notifyWhenReceivedMessage(InstantiateVolumeMsg.class) { InstantiateVolumeMsg msg ->
            if (!(msg instanceof InstantiateRootVolumeMsg)) {
                allocatedDataPsUuid = msg.primaryStorageUuid
            }
        }

        try {
            DiskAO rootDisk = DiskAO.rootDisk()
            rootDisk.size = SizeUnit.GIGABYTE.toByte(1)
            rootDisk.primaryStorageUuid = supportedPs.uuid
            rootDisk.platform = "Linux"
            rootDisk.guestOsType = "CentOS"
            rootDisk.architecture = "x86_64"
            DiskAO dataDisk = DiskAO.nonRootDisk()
            dataDisk.size = SizeUnit.GIGABYTE.toByte(1)
            dataDisk.encrypted = true

            expectApiFailure({
                createVmInstance {
                    name = "vm-with-encrypted-data-disk"
                    instanceOfferingUuid = (env.inventoryByName("instance-offering") as InstanceOfferingInventory).uuid
                    zoneUuid = supportedPs.zoneUuid
                    clusterUuid = (env.inventoryByName("cluster") as ClusterInventory).uuid
                    hostUuid = (env.inventoryByName("kvm") as HostInventory).uuid
                    l3NetworkUuids = [(env.inventoryByName("l3") as L3NetworkInventory).uuid]
                    defaultL3NetworkUuid = (env.inventoryByName("l3") as L3NetworkInventory).uuid
                    delegate.diskAOs = [rootDisk, dataDisk]
                }
            }) {
                assert details.contains("no key provider") : \
                        "Encrypted data disk VM creation should reach encryption after PS allocation: details=${details}"
            }
        } finally {
            cleanup()
        }

        assert allocatedDataPsUuid == supportedPs.uuid : \
                "encrypted data disk auto allocation selected unsupported ZHPS: expected=${supportedPs.uuid} actual=${allocatedDataPsUuid}"
    }

    void testKeepPlainRootExplicitZhpsWhenEncryptedDataDiskAutoAllocation() {
        String allocatedRootPsUuid = null
        String allocatedDataPsUuid = null

        def cleanup = notifyWhenReceivedMessage(InstantiateVolumeMsg.class) { InstantiateVolumeMsg msg ->
            VolumeType volumeType = Q.New(VolumeVO.class)
                    .select(VolumeVO_.type)
                    .eq(VolumeVO_.uuid, msg.volumeUuid)
                    .findValue()
            if (volumeType == VolumeType.Root) {
                allocatedRootPsUuid = msg.primaryStorageUuid
            } else if (volumeType == VolumeType.Data) {
                allocatedDataPsUuid = msg.primaryStorageUuid
            }
        }

        try {
            DiskAO rootDisk = DiskAO.rootDisk()
            rootDisk.size = SizeUnit.GIGABYTE.toByte(1)
            rootDisk.primaryStorageUuid = zhps.uuid
            rootDisk.platform = "Linux"
            rootDisk.guestOsType = "CentOS"
            rootDisk.architecture = "x86_64"
            DiskAO dataDisk = DiskAO.nonRootDisk()
            dataDisk.size = SizeUnit.GIGABYTE.toByte(1)
            dataDisk.encrypted = true

            expectApiFailure({
                createVmInstance {
                    name = "vm-with-plain-root-on-zhps-and-encrypted-data"
                    instanceOfferingUuid = (env.inventoryByName("instance-offering") as InstanceOfferingInventory).uuid
                    zoneUuid = supportedPs.zoneUuid
                    clusterUuid = (env.inventoryByName("cluster") as ClusterInventory).uuid
                    hostUuid = (env.inventoryByName("kvm") as HostInventory).uuid
                    l3NetworkUuids = [(env.inventoryByName("l3") as L3NetworkInventory).uuid]
                    defaultL3NetworkUuid = (env.inventoryByName("l3") as L3NetworkInventory).uuid
                    delegate.diskAOs = [rootDisk, dataDisk]
                }
            }) {
                assert details.contains("no key provider") : \
                        "Encrypted data disk VM creation should keep explicit plain root PS and reach encryption: details=${details}"
            }
        } finally {
            cleanup()
        }

        assert allocatedRootPsUuid == zhps.uuid : \
                "plain root with explicit ZHPS should not be filtered by encrypted data disk: expected=${zhps.uuid} actual=${allocatedRootPsUuid}"
        assert allocatedDataPsUuid == supportedPs.uuid : \
                "encrypted data disk auto allocation should filter ZHPS independently: expected=${supportedPs.uuid} actual=${allocatedDataPsUuid}"
    }

    void testFilterZhpsForEncryptedTemplateDataVolumeAutoAllocation() {
        HostInventory host = env.inventoryByName("kvm") as HostInventory
        ClusterInventory cluster = env.inventoryByName("cluster") as ClusterInventory
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory
        InstanceOfferingInventory offering = env.inventoryByName("instance-offering") as InstanceOfferingInventory
        ImageInventory dataVolumeTemplate = env.inventoryByName("data-volume-template") as ImageInventory

        DiskAO rootDisk = DiskAO.rootDisk()
        rootDisk.size = SizeUnit.GIGABYTE.toByte(1)
        rootDisk.primaryStorageUuid = supportedPs.uuid
        rootDisk.platform = "Linux"
        rootDisk.guestOsType = "CentOS"
        rootDisk.architecture = "x86_64"
        DiskAO dataDisk = DiskAO.nonRootDisk().withImage(dataVolumeTemplate.uuid)
        dataDisk.encrypted = true

        int nfsDataVolumeDownloads = 0
        env.afterSimulator(NfsPrimaryToSftpBackupKVMBackend.DOWNLOAD_FROM_SFTP_PATH) { rsp, e ->
            nfsDataVolumeDownloads++
            return rsp
        }

        expectApiFailure({
            createVmInstance {
                name = "vm-with-encrypted-template-data-volume"
                instanceOfferingUuid = offering.uuid
                zoneUuid = supportedPs.zoneUuid
                clusterUuid = cluster.uuid
                hostUuid = host.uuid
                l3NetworkUuids = [l3.uuid]
                defaultL3NetworkUuid = l3.uuid
                delegate.diskAOs = [rootDisk, dataDisk]
            }
        }) {
            assert details.contains("no key provider bound and no default key provider configured")
        }

        assert nfsDataVolumeDownloads == 1 : \
                "encrypted template data volume was not instantiated on non-ZHPS: NFS download count=${nfsDataVolumeDownloads}"
    }

    @Override
    void clean() {
        env.delete()
    }
}
