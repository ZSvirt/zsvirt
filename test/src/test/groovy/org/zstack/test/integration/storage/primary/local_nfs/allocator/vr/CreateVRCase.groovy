package org.zstack.test.integration.storage.primary.local_nfs.allocator.vr

import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.storage.primary.PrimaryStorageStateEvent
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.ApplianceVmInventory
import org.zstack.sdk.BackupStorageInventory
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.CreateVmInstanceAction
import org.zstack.sdk.HostInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VirtualRouterOfferingInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.util.ZStackTestUil
import org.zstack.utils.data.SizeUnit

/**
 * Created by lining on 2017/10/17.
 */
class CreateVRCase extends SubCase {
    EnvSpec env
    HostInventory host1
    HostInventory host2
    ImageInventory vrImage_size_8G
    ClusterInventory cluster
    ImageInventory image
    InstanceOfferingInventory instanceOffering
    L3NetworkInventory l3
    PrimaryStorageInventory nfs_200G
    PrimaryStorageInventory nfs_80G
    PrimaryStorageInventory local
    PrimaryStorageInventory local2
    PrimaryStorageInventory nfs

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
                diskSize = SizeUnit.GIGABYTE.toByte(1)
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image"
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
                        managementIp = "localhost"
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
                    attachPrimaryStorage("local2")
                    attachL2Network("l2")
                }

                nfsPrimaryStorage {
                    name = "nfs"
                    url = "localhost:/nfs"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                    availableCapacity = SizeUnit.GIGABYTE.toByte(100)
                }

                nfsPrimaryStorage {
                    name = "nfs2"
                    url = "localhost:/nfs2"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                    availableCapacity = SizeUnit.GIGABYTE.toByte(100)
                }

                nfsPrimaryStorage {
                    name = "nfs_200G"
                    url = "localhost:/nfs3"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(202)
                    availableCapacity = SizeUnit.GIGABYTE.toByte(202)
                }

                nfsPrimaryStorage {
                    name = "nfs_80G"
                    url = "localhost:/nfs4"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(80)
                    availableCapacity = SizeUnit.GIGABYTE.toByte(80)
                }

                localPrimaryStorage {
                    name = "local2"
                    url = "/local_ps2"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                    availableCapacity = SizeUnit.GIGABYTE.toByte(100)
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(120)
                    availableCapacity = SizeUnit.GIGABYTE.toByte(120)
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
        }
    }

    @Override
    void test() {
        env.create {

            BackupStorageInventory bs = env.inventoryByName("sftp")
            long imageSize = SizeUnit.GIGABYTE.toByte(8)
            vrImage_size_8G = ZStackTestUil.addImageToSftpBackupStorage(env, imageSize, imageSize, bs.uuid)
            VirtualRouterOfferingInventory vrOffering = env.inventoryByName("vr")
            updateVirtualRouterOffering {
                uuid = vrOffering.uuid
                imageUuid = vrImage_size_8G.uuid
            }

            localAndLocal()
            localAndNfs()
            nfsAndNfs()
        }
    }

    void localAndLocal() {
        cluster = env.inventoryByName("cluster")
        local = env.inventoryByName("local") as PrimaryStorageInventory
        local2 = env.inventoryByName("local2") as PrimaryStorageInventory
        image = env.inventoryByName("image")
        instanceOffering = env.inventoryByName("instanceOffering")
        l3 = env.inventoryByName("l3")

        changePrimaryStorageState {
            uuid = local.uuid
            stateEvent = PrimaryStorageStateEvent.disable.toString()
        }

        VmInstanceInventory vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
        }

        ApplianceVmInventory vr = queryApplianceVm {
        }[0]
        ZStackTestUil.checkVmRootDiskPs(vr, local2.uuid)

        destroyVmInstance {
            uuid = vm.uuid
        }
        destroyVmInstance {
            uuid = vr.uuid
        }

        changePrimaryStorageState {
            uuid = local2.uuid
            stateEvent = PrimaryStorageStateEvent.disable.toString()
        }
        CreateVmInstanceAction action = new CreateVmInstanceAction(
                name: "vm2",
                instanceOfferingUuid: instanceOffering.uuid,
                imageUuid: image.uuid,
                l3NetworkUuids: [l3.uuid],
                sessionId: currentEnvSpec.session.uuid
        )
        assert null != action.call()

        detachPrimaryStorageFromCluster {
            clusterUuid = cluster.uuid
            primaryStorageUuid = local2.uuid
        }

        changePrimaryStorageState {
            uuid = local2.uuid
            stateEvent = PrimaryStorageStateEvent.enable.toString()
        }

        changePrimaryStorageState {
            uuid = local.uuid
            stateEvent = PrimaryStorageStateEvent.enable.toString()
        }
    }

    void localAndNfs(){
        nfs = env.inventoryByName("nfs") as PrimaryStorageInventory
        nfs_200G = env.inventoryByName("nfs_200G") as PrimaryStorageInventory
        nfs_80G = env.inventoryByName("nfs_80G") as PrimaryStorageInventory
        PrimaryStorageInventory local = env.inventoryByName("local") as PrimaryStorageInventory

        ClusterInventory cluster = env.inventoryByName("cluster")
        ImageInventory image = env.inventoryByName("image")
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        L3NetworkInventory l3 = env.inventoryByName("l3")

        attachPrimaryStorageToCluster {
            clusterUuid = cluster.uuid
            primaryStorageUuid = nfs.uuid
        }

        // local disable, nfs normal
        changePrimaryStorageState {
            uuid = local.uuid
            stateEvent = PrimaryStorageStateEvent.disable.toString()
        }

        VmInstanceInventory vm = createVmInstance {
            name = "vm3"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            primaryStorageUuidForRootVolume = nfs.uuid
        }

        ApplianceVmInventory vr = queryApplianceVm {
        }[0]
        ZStackTestUil.checkVmRootDiskPs(vr, nfs.uuid)

        destroyVmInstance {
            uuid = vm.uuid
        }
        destroyVmInstance {
            uuid = vr.uuid
        }

        // local disable, nfs disable
        changePrimaryStorageState {
            uuid = nfs.uuid
            stateEvent = PrimaryStorageStateEvent.maintain.toString()
        }
        expectCreateVmFail()

        // local normal, nfs disable
        changePrimaryStorageState {
            uuid = local.uuid
            stateEvent = PrimaryStorageStateEvent.enable.toString()
        }

        vm = createVmInstance {
            name = "vm4"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
        }

        vr = queryApplianceVm {
        }[0]
        ZStackTestUil.checkVmRootDiskPs(vr, local.uuid)

        destroyVmInstance {
            uuid = vm.uuid
        }
        destroyVmInstance {
            uuid = vr.uuid
        }

        // local normal, nfs normal
        changePrimaryStorageState {
            uuid = nfs.uuid
            stateEvent = PrimaryStorageStateEvent.enable.toString()
        }
        vm = createVmInstance {
            name = "vm5"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
        }
        vr = queryApplianceVm {
        }[0]
        destroyVmInstance {
            uuid = vm.uuid
        }
        destroyVmInstance {
            uuid = vr.uuid
        }

        // local normal, capacity not Enough
        // nfs normal, capacity not Enough
        BackupStorageInventory bs = env.inventoryByName("sftp")
        long imageSize = SizeUnit.GIGABYTE.toByte(100)
        ImageInventory vrImage_100G = ZStackTestUil.addImageToSftpBackupStorage(env, imageSize, imageSize, bs.uuid)
        VirtualRouterOfferingInventory vrOffering = env.inventoryByName("vr")
        updateVirtualRouterOffering {
            uuid = vrOffering.uuid
            imageUuid = vrImage_100G.uuid
        }

        CreateVmInstanceAction action = new CreateVmInstanceAction(
                name: "vm6",
                instanceOfferingUuid: instanceOffering.uuid,
                imageUuid: image.uuid,
                l3NetworkUuids: [l3.uuid],
                sessionId: currentEnvSpec.session.uuid
        )
        assert null != action.call()


        /*
        TODO: host allocate bug (nfs+local)
        // local normal, capacity Enough
        // nfs normal, capacity not Enough
        detachPrimaryStorageFromCluster {
            clusterUuid = cluster.uuid
            primaryStorageUuid = nfs.uuid
        }
        attachPrimaryStorageToCluster {
            clusterUuid = cluster.uuid
            primaryStorageUuid = nfs_80G.uuid
        }

        imageSize = SizeUnit.GIGABYTE.toByte(90)
        ImageInventory vrImage_90G = ZStackTestUil.addImageToSftpBackupStorage(env, imageSize, imageSize, bs.uuid)
        updateVirtualRouterOffering {
            uuid = vrOffering.uuid
            imageUuid = vrImage_90G.uuid
        }
        vm = createVmInstance {
            name = "vm7"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
        }
        vr = queryApplianceVm {
        }[0]
        ZStackTestUil.checkVmRootDiskPs(vr, local.uuid)
        destroyVmInstance {
            uuid = vm.uuid
        }
        destroyVmInstance {
            uuid = vr.uuid
        }
        */

        // local normal, capacity not Enough
        // nfs normal, capacity Enough
        attachPrimaryStorageToCluster {
            clusterUuid = cluster.uuid
            primaryStorageUuid = nfs_200G.uuid
        }

        /*
        TODO: host allocate bug (nfs+local)
        updateVirtualRouterOffering {
            uuid = vrOffering.uuid
            imageUuid = vrImage_100G.uuid
        }

        vm = createVmInstance {
            name = "vm8"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
        }
        vr = queryApplianceVm {
        }[0]
        ZStackTestUil.checkVmRootDiskPs(vr, nfs_200G.uuid)
        destroyVmInstance {
            uuid = vm.uuid
        }
        destroyVmInstance {
            uuid = vr.uuid
        }
        */

        updateVirtualRouterOffering {
            uuid = vrOffering.uuid
            imageUuid = vrImage_size_8G.uuid
        }


    }

    void nfsAndNfs(){
        detachPrimaryStorageFromCluster {
            clusterUuid = cluster.uuid
            primaryStorageUuid = local.uuid
        }

        VmInstanceInventory vm = createVmInstance {
            name = "vm9"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
        }

        ApplianceVmInventory vr = queryApplianceVm {
        }[0]

        destroyVmInstance {
            uuid = vm.uuid
        }
        destroyVmInstance {
            uuid = vr.uuid
        }


        changePrimaryStorageState {
            uuid = nfs_200G.uuid
            stateEvent = PrimaryStorageStateEvent.disable.toString()
        }
        vm = createVmInstance {
            name = "vm10"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
        }

        vr = queryApplianceVm {
        }[0]
        ZStackTestUil.checkVmRootDiskPs(vr, nfs.uuid)

        destroyVmInstance {
            uuid = vm.uuid
        }
        destroyVmInstance {
            uuid = vr.uuid
        }


        changePrimaryStorageState {
            uuid = nfs_200G.uuid
            stateEvent = PrimaryStorageStateEvent.enable.toString()
        }
        changePrimaryStorageState {
            uuid = nfs.uuid
            stateEvent = PrimaryStorageStateEvent.disable.toString()
        }
        vm = createVmInstance {
            name = "vm11"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
        }

        vr = queryApplianceVm {
        }[0]
        ZStackTestUil.checkVmRootDiskPs(vr, nfs_200G.uuid)

        destroyVmInstance {
            uuid = vm.uuid
        }
        destroyVmInstance {
            uuid = vr.uuid
        }

        detachPrimaryStorageFromCluster {
            clusterUuid = cluster.uuid
            primaryStorageUuid = nfs.uuid
        }

        detachPrimaryStorageFromCluster {
            clusterUuid = cluster.uuid
            primaryStorageUuid = nfs_200G.uuid
        }
    }

    private void expectCreateVmFail(){
        ClusterInventory cluster = env.inventoryByName("cluster")
        ImageInventory image = env.inventoryByName("image")
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        L3NetworkInventory l3 = env.inventoryByName("l3")

        CreateVmInstanceAction action = new CreateVmInstanceAction(
                name: "vm2",
                instanceOfferingUuid: instanceOffering.uuid,
                imageUuid: image.uuid,
                l3NetworkUuids: [l3.uuid],
                sessionId: currentEnvSpec.session.uuid
        )
        assert null != action.call()
    }
}
