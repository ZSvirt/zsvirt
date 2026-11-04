package org.zstack.test.integration.storage.metadata

import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.core.Platform
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.header.message.MessageReply
import org.zstack.header.storage.primary.CleanupVmInstanceMetadataOnPrimaryStorageMsg
import org.zstack.header.storage.primary.GetVmInstanceMetadataFromPrimaryStorageMsg
import org.zstack.header.storage.primary.RebaseVolumeBackingFileOnPrimaryStorageMsg
import org.zstack.header.vm.metadata.UpdateVmInstanceMetadataMsg
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.header.vm.VmInstanceVO_
import org.zstack.header.vm.metadata.ResourceMetadata
import org.zstack.header.vm.metadata.VmInstanceMetadataDTO
import org.zstack.header.vm.metadata.VmMetadataCategory
import org.zstack.header.vm.metadata.VolumeResourceMetadata
import org.zstack.header.volume.VolumeState
import org.zstack.header.volume.VolumeStatus
import org.zstack.header.volume.VolumeType
import org.zstack.header.volume.VolumeVO
import org.zstack.header.volume.VolumeVO_
import org.zstack.sdk.RegisterVmInstanceFromMetadataAction
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

/** Inline registration must work without a storage metadata provider or physical disk operations. */
class InlineVmMetadataRegistrationCase extends SubCase {
    EnvSpec env
    List<String> storageRequests = Collections.synchronizedList(new ArrayList<String>())
    Boolean originalMetadataEnabled
    Set<Long> allocatedInternalIds = new HashSet<>()
    private final List<Class> storageMessageTypes = [GetVmInstanceMetadataFromPrimaryStorageMsg,
            RebaseVolumeBackingFileOnPrimaryStorageMsg, CleanupVmInstanceMetadataOnPrimaryStorageMsg,
            UpdateVmInstanceMetadataMsg]

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
            zone {
                name = "zone"
                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"
                    kvm {
                        name = "host"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }
                    attachPrimaryStorage("ceph")
                }
                cephPrimaryStorage {
                    name = "ceph"
                    fsid = "7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls = ["root:password@127.0.0.1"]
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            originalMetadataEnabled = VmGlobalConfig.VM_METADATA_ENABLED.value(Boolean.class)
            VmGlobalConfig.VM_METADATA_ENABLED.updateValue(true)
            storageMessageTypes.each { type ->
                env.message(type) { msg, CloudBus bus ->
                    storageRequests.add(msg.class.simpleName)
                    MessageReply reply = new MessageReply()
                    reply.error = Platform.operr("inline registration must not issue storage metadata requests")
                    bus.reply(msg, reply)
                }
            }
            registerAllVolumes(false)
            registerAllVolumes(true)
            assert storageRequests.empty
        }
    }

    private void registerAllVolumes(boolean regenerate) {
        String vmUuid = Platform.uuid
        String rootUuid = Platform.uuid
        String dataUuid = Platform.uuid
        String psUuid = env.inventoryByName("ceph").uuid
        VmInstanceVO vm = new VmInstanceVO()
        vm.uuid = vmUuid
        vm.name = "inline-registration"
        vm.cpuNum = 2
        vm.memorySize = 1073741824L
        vm.hypervisorType = "KVM"
        vm.platform = "Linux"
        vm.type = "UserVm"
        vm.state = VmInstanceState.Stopped
        vm.rootVolumeUuid = rootUuid

        VmInstanceMetadataDTO dto = new VmInstanceMetadataDTO()
        dto.schemaVersion = bean(DatabaseFacade.class).dbVersion
        dto.vmCategory = VmMetadataCategory.VM
        dto.vm = new ResourceMetadata(resourceUuid: vmUuid, vo: JSONObjectUtil.toJsonString(vm))
        // The source ID is optional; registration always allocates a target-local ID.
        assert !JSONObjectUtil.toObject(dto.vm.vo, Map.class).containsKey("internalId")
        dto.volumes = [volume(rootUuid, vmUuid, psUuid, VolumeType.Root, 0),
                       volume(dataUuid, vmUuid, psUuid, VolumeType.Data, 1)]
        Set<String> paths = dto.volumes.collect { JSONObjectUtil.toObject(it.vo, VolumeVO.class).installPath } as Set

        RegisterVmInstanceFromMetadataAction action = new RegisterVmInstanceFromMetadataAction()
        action.metadata = JSONObjectUtil.toJsonString(dto)
        action.primaryStorageUuid = psUuid
        action.zoneUuid = env.inventoryByName("zone").uuid
        action.clusterUuid = env.inventoryByName("cluster").uuid
        action.hostUuid = env.inventoryByName("host").uuid
        action.regenerateUuid = regenerate
        action.sessionId = adminSession()
        def result = action.call()
        assert result.error == null: result.error
        def registered = result.value.inventory
        assert registered.state == "Stopped"
        VmInstanceVO registeredVo = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, registered.uuid).find()
        assert registeredVo.internalId > 0
        assert allocatedInternalIds.add(registeredVo.internalId)
        assert regenerate ? registered.uuid != vmUuid : registered.uuid == vmUuid
        List<VolumeVO> volumes = Q.New(VolumeVO.class).eq(VolumeVO_.vmInstanceUuid, registered.uuid).list()
        assert volumes.size() == 2
        assert volumes.collect { it.installPath }.toSet() == paths
        assert volumes.every { it.primaryStorageUuid == psUuid && it.status == VolumeStatus.Ready }
        assert volumes.find { it.type == VolumeType.Root }.uuid == registered.rootVolumeUuid
        assert volumes.collect { it.deviceId }.toSet() == [0, 1].toSet()
        assert storageRequests.empty

        // The existing owner must survive a second attempt to adopt the same physical paths.
        action.regenerateUuid = true
        assert action.call().error != null
        assert Q.New(VolumeVO.class).eq(VolumeVO_.vmInstanceUuid, registered.uuid).count() == 2
        assert storageRequests.empty
    }

    private VolumeResourceMetadata volume(String uuid, String vmUuid, String psUuid, VolumeType type, int deviceId) {
        VolumeVO vo = new VolumeVO()
        vo.uuid = uuid
        vo.name = "inline-${deviceId}"
        vo.vmInstanceUuid = vmUuid
        vo.primaryStorageUuid = psUuid
        vo.installPath = "ceph://existing-pool/${uuid}"
        vo.type = type
        vo.deviceId = deviceId
        vo.format = "raw"
        vo.size = 1073741824L
        vo.actualSize = 1073741824L
        vo.state = VolumeState.Enabled
        vo.status = VolumeStatus.Ready
        return new VolumeResourceMetadata(resourceUuid: uuid, vo: JSONObjectUtil.toJsonString(vo))
    }

    @Override
    void clean() {
        storageMessageTypes.each { env.revokeMessage(it, null) }
        if (originalMetadataEnabled != null) {
            VmGlobalConfig.VM_METADATA_ENABLED.updateValue(originalMetadataEnabled)
        }
        env.delete()
    }
}
