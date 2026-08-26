package org.zstack.testlib

import org.zstack.header.vm.DiskAO
import org.zstack.utils.data.SizeUnit

class VmDiskSpec extends Spec {
    private Closure primaryStorage = {}
    private Closure diskOffering = {}

    @SpecParam(required = false)
    boolean boot
    @SpecParam(required = false)
    String platform
    @SpecParam(required = false)
    String guestOsType
    @SpecParam(required = false)
    String architecture
    @SpecParam(required = false)
    long size
    /**
     * allow: ImageVO.uuid
     */
    @SpecParam(required = false)
    String templateUuid
    @SpecParam(required = false)
    String sourceType
    /**
     * allow: VolumeVO.uuid
     */
    @SpecParam(required = false)
    String sourceUuid
    @SpecParam(required = false)
    List<String> systemTags = []
    @SpecParam(required = false)
    String name

    String virtualUuid
    String virtualName

    VmDiskSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    @Override
    SpecID create(String uuid, String sessionId) {
        virtualName = name == null ? "VmDiskSpec-" + uuid : name
        return id(virtualName, virtualUuid = uuid)
    }

    @Override
    void delete(String sessionId) {
        // do-nothing
    }

    DiskAO toDiskAO() {
        DiskAO ao = new DiskAO()
        ao.boot = boot
        ao.platform = platform
        ao.guestOsType = guestOsType
        ao.architecture = architecture
        ao.primaryStorageUuid = primaryStorage()
        ao.size = size
        ao.templateUuid = templateUuid
        ao.diskOfferingUuid = diskOffering()
        ao.sourceType = sourceType
        ao.sourceUuid = sourceUuid
        ao.systemTags = new ArrayList<>(systemTags)
        ao.name = name
        return ao
    }

    void sizeGB(long sizeGB) {
        this.size = SizeUnit.GIGABYTE.toByte(sizeGB)
    }

    @SpecMethod
    void usePrimaryStorage(String name) {
        preCreate {
            addDependency(name, PrimaryStorageSpec.class)
        }
        primaryStorage = {
            PrimaryStorageSpec spec = findSpec(name, PrimaryStorageSpec.class)
            assert spec != null: "cannot find primaryStorage[$name], check the vm block of environment"
            return spec.inventory.uuid
        }
    }

    @SpecMethod
    void useDiskOffering(String name) {
        preCreate {
            addDependency(name, DiskOfferingSpec.class)
        }

        diskOffering = {
            def spec = findSpec(name, DiskOfferingSpec.class) as DiskOfferingSpec
            assert spec != null: "cannot find diskOffering[$name], check the disk block of environment"
            return spec.inventory.uuid
        }
    }
}