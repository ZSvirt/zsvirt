package org.zstack.test.integration.storage.primary.addon.expon

import org.springframework.http.HttpEntity
import org.zstack.core.Platform
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.core.singleflight.MultiNodeSingleFlightImpl
import org.zstack.expon.ExponApiHelper
import org.zstack.expon.ExponStorageController
import org.zstack.expon.sdk.iscsi.IscsiClientGroupModule
import org.zstack.expon.sdk.iscsi.IscsiModule
import org.zstack.expon.sdk.vhost.VhostControllerModule
import org.zstack.header.host.HostConstant
import org.zstack.header.host.PingHostMsg
import org.zstack.header.message.MessageReply
import org.zstack.header.storage.backup.DownloadImageFromRemoteTargetMsg
import org.zstack.header.storage.backup.DownloadImageFromRemoteTargetReply
import org.zstack.header.storage.backup.UploadImageToRemoteTargetReply
import org.zstack.header.storage.backup.UploadImageToRemoteTargetMsg
import org.zstack.header.storage.primary.ImageCacheShadowVO
import org.zstack.header.storage.primary.ImageCacheShadowVO_
import org.zstack.header.storage.primary.ImageCacheVO
import org.zstack.header.storage.primary.ImageCacheVO_
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO_
import org.zstack.header.vm.VmBootDevice
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.header.vm.VmStateChangedOnHostMsg
import org.zstack.header.vm.devices.DeviceAddress
import org.zstack.header.vm.devices.VirtualDeviceInfo
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMGlobalConfig
import org.zstack.kvm.VolumeTO
import org.zstack.sdk.*
import org.zstack.storage.addon.primary.ExternalPrimaryStorageFactory
import org.zstack.storage.backup.BackupStorageSystemTags
import org.zstack.tag.SystemTagCreator
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.CollectionUtils
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import static java.util.Arrays.asList
import static org.zstack.expon.ExponIscsiHelper.iscsiExportTargetName
import static org.zstack.expon.ExponNameHelper.getVolIdFromPath

class ExponPrimaryStorageCase extends SubCase {
    EnvSpec env
    ClusterInventory cluster
    InstanceOfferingInventory instanceOffering
    DiskOfferingInventory diskOffering
    ImageInventory image, iso
    L3NetworkInventory l3
    PrimaryStorageInventory ps
    BackupStorageInventory bs
    VmInstanceInventory vm
    VolumeInventory vol, vol2
    HostInventory host1, host2
    CloudBus bus
    ExponStorageController controller
    ExponApiHelper apiHelper

    String exponUrl = "https://admin:Admin123@172.25.108.64:443/pool"
    String exportProtocol = "iscsi://"

    @Override
    void clean() {
        System.setProperty("useImageSpecSize", "false")
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
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
            }

            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(2)
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "127.0.0.2"

                image {
                    name = "image"
                    url = "http://zstack.org/download/test.qcow2"
                    size = SizeUnit.GIGABYTE.toByte(1)
                    virtio = true
                }

                image {
                    name = "iso"
                    url = "http://zstack.org/download/test.iso"
                    size = SizeUnit.GIGABYTE.toByte(1)
                    format = "iso"
                    virtio = true
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
                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.3"
                        username = "root"
                        password = "password"
                    }

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

                attachBackupStorage("sftp")
            }
        }
    }

    @Override
    void test() {
        System.setProperty("useImageSpecSize", "true")
        env.create {
            if (System.getProperty("inTestSuite") != null) {
                logger.debug("skip expon case in test suite")
                return
            }

            cluster = env.inventoryByName("cluster") as ClusterInventory
            instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
            diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory
            image = env.inventoryByName("image") as ImageInventory
            iso = env.inventoryByName("iso") as ImageInventory
            l3 = env.inventoryByName("l3") as L3NetworkInventory
            bs = env.inventoryByName("sftp") as BackupStorageInventory
            host1 = env.inventoryByName("kvm") as HostInventory
            host2 = env.inventoryByName("kvm2") as HostInventory
            bus = bean(CloudBus.class)

            KVMGlobalConfig.VM_SYNC_ON_HOST_PING.updateValue(true)
            simulatorEnv()
            testCreateExponStorage()
            testComplexPassword()
            testSessionExpired()
            testCreateVm()
            testHandleInactiveVolume()
            testCreateVolumeRollback()
            testAttachIso()
            testExponIscsiAttach()
            testExpungeActiveVolume()
            testCreateDataVolume()
            testCreateSnapshot()
            testCreateTemplate()
            testClean()
            testImageCacheClean()

            reconnectPrimaryStorage {
                uuid = ps.uuid
            }

            testDeletePs()
        }
    }

    void simulatorEnv() {
        //TODO mock all
        env.afterSimulator(KVMConstant.KVM_ATTACH_VOLUME) { KVMAgentCommands.AttachDataVolumeResponse rsp, HttpEntity<String> e ->
            KVMAgentCommands.AttachDataVolumeCmd cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.AttachDataVolumeCmd.class)

            VirtualDeviceInfo info = new VirtualDeviceInfo()
            info.resourceUuid = cmd.volume.resourceUuid
            info.deviceAddress = new DeviceAddress()
            info.deviceAddress.domain = "0000"
            info.deviceAddress.bus = "00"
            info.deviceAddress.slot = Long.toHexString(Q.New(VolumeVO.class).eq(VolumeVO_.vmInstanceUuid, cmd.vmUuid).count())
            info.deviceAddress.function = "0"

            rsp.virtualDeviceInfoList = []
            rsp.virtualDeviceInfoList.addAll(info)
            return rsp
        }

        SystemTagCreator creator = BackupStorageSystemTags.ISCSI_INITIATOR_NAME.newSystemTagCreator(bs.uuid);
        creator.setTagByTokens(Collections.singletonMap(BackupStorageSystemTags.ISCSI_INITIATOR_NAME_TOKEN, "iqn.1994-05.com.redhat:fc16b4d4fb3f"));
        creator.inherent = false;
        creator.recreate = true;
        creator.create();
    }

    void testCreateExponStorage() {
        def zone = env.inventoryByName("zone") as ZoneInventory

        discoverExternalPrimaryStorage {
            url = exponUrl
            identity = "expon"
        }

        ps = addExternalPrimaryStorage {
            name = "test"
            zoneUuid = zone.uuid
            url = exponUrl
            identity = "expon"
            config = ""
            defaultOutputProtocol = "Vhost"
        } as ExternalPrimaryStorageInventory

        assert !ps.url.contains("Admin123")

        updateExternalPrimaryStorage {
            uuid = ps.uuid
            config = '''{"pools":[{"name":"pool", "aliasName":"test"}]}'''
        }

        ps = queryPrimaryStorage {}[0] as ExternalPrimaryStorageInventory
        assert ps.getAddonInfo() != null

        def psRet = zqlQuery("query primarystorage")[0]
        assert !psRet.url.contains("Admin123")

        attachPrimaryStorageToCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = cluster.uuid
        }

        ExternalPrimaryStorageFactory factory = Platform.getComponentLoader().getComponent(ExternalPrimaryStorageFactory.class)
        controller = factory.getControllerSvc(ps.uuid) as ExponStorageController
        apiHelper = controller.apiHelper

        PingHostMsg pmsg = new PingHostMsg()
        pmsg.hostUuid = host1.uuid
        bus.makeTargetServiceIdByResourceUuid(pmsg, HostConstant.SERVICE_ID, host1.uuid)
        MessageReply r = bus.call(pmsg)
        assert r.success
        assert Q.New(PrimaryStorageHostRefVO.class).eq(PrimaryStorageHostRefVO_.hostUuid, host1.uuid).find().status.toString() == "Connected"

        pmsg = new PingHostMsg()
        pmsg.hostUuid = host2.uuid
        bus.makeTargetServiceIdByResourceUuid(pmsg, HostConstant.SERVICE_ID, host2.uuid)
        r = bus.call(pmsg)
        assert r.success
        assert Q.New(PrimaryStorageHostRefVO.class).eq(PrimaryStorageHostRefVO_.hostUuid, host2.uuid).find().status.toString() == "Disconnected"

        // ping again
        pmsg = new PingHostMsg()
        pmsg.hostUuid = host1.uuid
        bus.makeTargetServiceIdByResourceUuid(pmsg, HostConstant.SERVICE_ID, host1.uuid)
        r = bus.call(pmsg)
        assert r.success
        assert Q.New(PrimaryStorageHostRefVO.class).eq(PrimaryStorageHostRefVO_.hostUuid, host1.uuid).find().status.toString() == "Connected"

        reconnectPrimaryStorage {
            uuid = ps.uuid
        }
    }

    void testComplexPassword() {
        String pswd = "Pswd@#123"
        String encodePswd = URLEncoder.encode(pswd, "UTF-8")
        discoverExternalPrimaryStorage {
            url = String.format("https://complex:%s@172.25.108.64:443/pool", encodePswd)
            identity = "expon"
        }
    }

    void testSessionExpired() {
        controller.apiHelper.sessionId = "invalid"
    }

    void testCreateVm() {
        def result = getCandidatePrimaryStoragesForCreatingVm {
            l3NetworkUuids = [l3.uuid]
            imageUuid = image.uuid
        } as GetCandidatePrimaryStoragesForCreatingVmResult

        assert result.getRootVolumePrimaryStorages().size() == 1

        env.message(UploadImageToRemoteTargetMsg.class){ UploadImageToRemoteTargetMsg msg, CloudBus bus ->
            UploadImageToRemoteTargetReply r = new  UploadImageToRemoteTargetReply()
            assert msg.getRemoteTargetUrl().startsWith(exportProtocol)
            assert msg.getFormat() == "raw"
            bus.reply(msg, r)
        }

        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.StartVmCmd.class)
            assert cmd.rootVolume.deviceType == VolumeTO.VHOST
            assert cmd.rootVolume.installPath.startsWith("/var/run")
            assert cmd.rootVolume.format == "raw"
            if (cmd.cdRoms != null) {
                cmd.cdRoms.forEach {
                    if (!it.isEmpty()) {
                        assert it.getPath().startsWith(exportProtocol)
                    }
                }
            }
            return rsp
        }

        // create vm concurrently
        boolean success = false
        Thread thread = new Thread(new Runnable() {
            @Override
            void run() {
                def otherVm = createVmInstance {
                    name = "vm"
                    instanceOfferingUuid = instanceOffering.uuid
                    rootDiskOfferingUuid = diskOffering.uuid
                    imageUuid = image.uuid
                    l3NetworkUuids = [l3.uuid]
                    hostUuid = host1.uuid
                } as VmInstanceInventory

                assert otherVm.allVolumes[0].size == diskOffering.diskSize
                assert apiHelper.getVolume(getVolIdFromPath(otherVm.allVolumes[0].installPath)).volumeSize == diskOffering.diskSize
                deleteVm(otherVm.uuid)
                success = true
            }
        })

        thread.run()
        vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            hostUuid = host1.uuid
        } as VmInstanceInventory

        thread.join()
        assert success

        stopVmInstance {
            uuid = vm.uuid
        }

        startVmInstance {
            uuid = vm.uuid
            hostUuid = host1.uuid
        }

        rebootVmInstance {
            uuid = vm.uuid
        }

        def vm2 = createVmInstance {
            name = "vm"
            instanceOfferingUuid = instanceOffering.uuid
            rootDiskOfferingUuid = diskOffering.uuid
            imageUuid = iso.uuid
            l3NetworkUuids = [l3.uuid]
            hostUuid = host1.uuid
        } as VmInstanceInventory

        deleteVm(vm2.uuid)
    }

    void testHandleInactiveVolume() {

        VhostControllerModule vhost = apiHelper.queryVhostController("volume-" + vm.rootVolumeUuid)

        assert !CollectionUtils.isEmpty(controller.apiHelper.getVhostControllerBoundUss(vhost.id))

        env.simulator(KVMConstant.KVM_VOLUME_SYNC_PATH) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.VolumeSyncCmd.class)
            assert cmd.storagePaths.get(0).endsWith("/volume-*")
            def rsp = new KVMAgentCommands.VolumeSyncRsp()
            rsp.inactiveVolumePaths = new HashMap<>()
            rsp.inactiveVolumePaths.put(cmd.storagePaths.get(0), asList("/var/run/wds/volume-" + vm.rootVolumeUuid))
            return rsp
        }

        def msg = new PingHostMsg()
        msg.hostUuid = host1.uuid
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host1.uuid)
        MessageReply r = bus.call(msg)
        assert r.success

        sleep(1000)
        // vm in running, not deactivate volume
        assert !CollectionUtils.isEmpty(controller.apiHelper.getVhostControllerBoundUss(vhost.id))

        SQL.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm.uuid).set(VmInstanceVO_.hostUuid, null).set(VmInstanceVO_.state, VmInstanceState.Starting).update()
        env.message(VmStateChangedOnHostMsg.class){ VmStateChangedOnHostMsg cmsg, CloudBus bus ->
            bus.reply(cmsg, new MessageReply())
        }
        r = bus.call(msg)
        assert r.success

        sleep(1000)
        // vm in starting, not deactivate volume
        assert !CollectionUtils.isEmpty(controller.apiHelper.getVhostControllerBoundUss(vhost.id))
        env.cleanMessageHandlers()

        SQL.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm.uuid).set(VmInstanceVO_.hostUuid, null).set(VmInstanceVO_.state, VmInstanceState.Stopped).update()
        r = bus.call(msg)
        assert r.success

        sleep(1000)
        // vm in stop, deactivate volume
        retryInSecs {
            assert CollectionUtils.isEmpty(controller.apiHelper.getVhostControllerBoundUss(vhost.id))
        }

        SQL.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm.uuid).set(VmInstanceVO_.hostUuid, host1.uuid).set(VmInstanceVO_.state, VmInstanceState.Running).update()
        env.simulator(KVMConstant.KVM_VOLUME_SYNC_PATH) { HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.VolumeSyncCmd.class)
            assert cmd.storagePaths.get(0).endsWith("/volume-*")
            return new KVMAgentCommands.VolumeSyncRsp()
        }
    }

    void testCreateVolumeRollback() {
        def vol = createDataVolume {
            name = "test"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ps.uuid
        } as VolumeInventory

        env.afterSimulator(KVMConstant.KVM_ATTACH_VOLUME) { rsp, HttpEntity<String> e ->
            rsp.setError("on purpose")
            return rsp
        }

        expectError {
            attachDataVolumeToVm {
                vmInstanceUuid = vm.uuid
                volumeUuid = vol.uuid
            }
        }

        env.afterSimulator(KVMConstant.KVM_ATTACH_VOLUME) { rsp, HttpEntity<String> e ->
            return rsp
        }

        deleteVolume(vol.uuid)
    }

    void testExpungeActiveVolume() {
        def vol = createDataVolume {
            name = "test"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ps.uuid
        } as VolumeInventory


        attachDataVolumeToVm {
            vmInstanceUuid = vm.uuid
            volumeUuid = vol.uuid
        }

        // skip deactivate volume
        SQL.New(VolumeVO.class).eq(VolumeVO_.uuid, vol.uuid).set(VolumeVO_.vmInstanceUuid, null).update()

        deleteVolume(vol.uuid)
    }

    void testAttachIso() {
        env.afterSimulator(KVMConstant.KVM_ATTACH_ISO_PATH) { rsp, HttpEntity<String> e ->
            def cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.AttachIsoCmd.class)
            assert cmd.iso.getPath().startsWith(exportProtocol)
            return rsp
        }

        attachIsoToVmInstance {
            vmInstanceUuid = vm.uuid
            isoUuid = iso.uuid
        }

        rebootVmInstance {
            uuid = vm.uuid
        }

        setVmBootOrder {
            uuid = vm.uuid
            bootOrder = asList(VmBootDevice.CdRom.toString(), VmBootDevice.HardDisk.toString(), VmBootDevice.Network.toString())
        }

        stopVmInstance {
            uuid = vm.uuid
        }

        startVmInstance {
            uuid = vm.uuid
            // hostUuid = host2.uuid
        }

        setVmBootOrder {
            uuid = vm.uuid
            bootOrder = asList(VmBootDevice.HardDisk.toString(), VmBootDevice.CdRom.toString(), VmBootDevice.Network.toString())
        }

        stopVmInstance {
            uuid = vm.uuid
        }

        startVmInstance {
            uuid = vm.uuid
            hostUuid = host1.uuid
        }

        detachIsoFromVmInstance {
            vmInstanceUuid = vm.uuid
        }
    }

    void testExponIscsiAttach() {
        def poolId = controller.addonInfo.getPools()[0].id
        def exponVol = controller.apiHelper.createVolume("test_" + Platform.uuid, poolId, SizeUnit.GIGABYTE.toByte(1))

        IscsiModule iscsi = apiHelper.queryIscsiController(iscsiExportTargetName)
        IscsiClientGroupModule client = apiHelper.queryIscsiClient("iscsi_127_0_0_2") // bs ip
        apiHelper.addVolumeToIscsiClientGroup(exponVol.id, client.id, iscsi.id, false)
        apiHelper.addVolumeToIscsiClientGroup(exponVol.id, client.id, iscsi.id, false)

        def snapshot = apiHelper.createVolumeSnapshot(exponVol.id, "test-" + Platform.uuid, "todo")

        apiHelper.addSnapshotToIscsiClientGroup(snapshot.id, client.id, iscsi.id)
        apiHelper.addSnapshotToIscsiClientGroup(snapshot.id, client.id, iscsi.id)


        apiHelper.removeVolumeFromIscsiClientGroup(exponVol.id, client.id)
        apiHelper.removeSnapshotFromIscsiClientGroup(snapshot.id, client.id)
        apiHelper.deleteVolume(exponVol.id, true)
    }

    void testCreateDataVolume() {
        vol = createDataVolume {
            name = "test"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ps.uuid
        } as VolumeInventory

        deleteVolume(vol.uuid)

        vol = createDataVolume {
            name = "test"
            diskOfferingUuid = diskOffering.uuid
            primaryStorageUuid = ps.uuid
        } as VolumeInventory

        attachDataVolumeToVm {
            vmInstanceUuid = vm.uuid
            volumeUuid = vol.uuid
        }

        vol2 = createDataVolume {
            name = "test"
            diskOfferingUuid = diskOffering.uuid
        } as VolumeInventory

        attachDataVolumeToVm {
            vmInstanceUuid = vm.uuid
            volumeUuid = vol2.uuid
        }

        detachDataVolumeFromVm {
            uuid = vol2.uuid
        }

        attachDataVolumeToVm {
            vmInstanceUuid = vm.uuid
            volumeUuid = vol2.uuid
        }
    }

    void testCreateSnapshot() {
        def snapshot = createVolumeSnapshot {
            name = "test"
            volumeUuid = vol.uuid
        } as VolumeSnapshotInventory

        stopVmInstance {
            uuid = vm.uuid
        }

        revertVolumeFromSnapshot {
            uuid = snapshot.uuid
        }

        deleteVolumeSnapshot {
            uuid = snapshot.uuid
        }

        startVmInstance {
            uuid = vm.uuid
        }

        /*
        def group = createVolumeSnapshotGroup {
            name = "test-snap"
            rootVolumeUuid = vm.rootVolumeUuid
        } as VolumeSnapshotGroupInventory

        stopVmInstance {
            uuid = vm.uuid
        }

        revertVmFromSnapshotGroup {
            uuid = group.uuid
        }

        deleteVolumeSnapshotGroup {
            uuid = group.uuid
        }

         */
    }

    void testCreateTemplate() {
        env.message(DownloadImageFromRemoteTargetMsg.class){ DownloadImageFromRemoteTargetMsg msg, CloudBus bus ->
            DownloadImageFromRemoteTargetReply r = new  DownloadImageFromRemoteTargetReply()
            assert msg.getRemoteTargetUrl().startsWith(exportProtocol)
            r.setInstallPath("zstore://test/image")
            r.setSize(100L)
            bus.reply(msg, r)
        }

        def dataImage = createDataVolumeTemplateFromVolume  {
            name = "vol-image"
            volumeUuid = vol.uuid
            backupStorageUuids = [bs.uuid]
        } as ImageInventory

        stopVmInstance {
            uuid = vm.uuid
        }

        def rootImage = createRootVolumeTemplateFromRootVolume {
            name = "root-image"
            rootVolumeUuid = vm.rootVolumeUuid
            backupStorageUuids = [bs.uuid]
        } as ImageInventory
    }

    void testClean() {
        deleteVm(vm.uuid)

        deleteDataVolume {
            uuid = vol.uuid
        }

        assert apiHelper.queryVhostController("volume-" + vol.uuid) != null
        expungeDataVolume {
            uuid = vol.uuid
        }

        assert apiHelper.queryVhostController("volume-" + vol.uuid) == null
    }

    void testImageCacheClean() {
        deleteImage {
            uuid = image.uuid
        }

        expungeImage {
            imageUuid = image.uuid
        }

        cleanUpImageCacheOnPrimaryStorage {
            uuid = ps.uuid
        }

        retryInSecs {
            assert Q.New(ImageCacheVO.class).eq(ImageCacheVO_.imageUuid, image.uuid).count() == 0
            assert Q.New(ImageCacheShadowVO.class).eq(ImageCacheShadowVO_.imageUuid, image.uuid).count() == 0
        }
    }

    void testDeletePs() {
        assert MultiNodeSingleFlightImpl.getExecutor(ps.uuid) != null

        detachPrimaryStorageFromCluster {
            primaryStorageUuid = ps.uuid
            clusterUuid = cluster.uuid
        }

        deletePrimaryStorage {
            uuid = ps.uuid
        }

        retryInSecs {
            assert MultiNodeSingleFlightImpl.getExecutor(ps.uuid) == null
        }
    }

    void deleteVm(String vmUuid) {
        destroyVmInstance {
            uuid = vmUuid
        }

        expungeVmInstance {
            uuid = vmUuid
        }
    }

    void deleteVolume(String volUuid) {
        deleteDataVolume {
            uuid = volUuid
        }

        expungeDataVolume {
            uuid = volUuid
        }
    }
}
