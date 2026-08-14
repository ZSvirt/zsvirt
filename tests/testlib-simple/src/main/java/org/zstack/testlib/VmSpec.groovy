package org.zstack.testlib

import org.zstack.header.vm.DiskAO
import org.zstack.sdk.AttachDataVolumeToVmAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil
/**
 * Created by xing5 on 2017/2/16.
 */
class VmSpec extends Spec implements HasSession {
    private Closure instanceOffering = {}
    private Closure image = {}
    private Closure rootDiskOffering = {}
    private Closure cluster = {}
    private Closure host = {}
    private Closure primaryStorage = {}
    private Closure diskOfferings = {}
    private Closure l3Networks = {}
    private Closure defaultL3Network = {}
    @SpecParam(required = true)
    String name
    @SpecParam(required = false)
    Integer cpu
    @SpecParam(required = false)
    Long memorySize
    @SpecParam
    String description
    @SpecParam(required = false)
    Boolean virtio
    @SpecParam
    List<String> rootVolumeSystemTags = []
    @SpecParam
    List<String> dataVolumeSystemTags = []
    private List<String> volumeToAttach = []
    private List<VmDiskSpec> disks = []

    VmInstanceInventory inventory

    VmSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    @SpecMethod
    void useInstanceOffering(String name) {
        preCreate {
            addDependency(name, InstanceOfferingSpec.class)
        }

        instanceOffering = {
            InstanceOfferingSpec spec = findSpec(name, InstanceOfferingSpec.class)
            assert spec != null: "cannot find instance offering[$name], check the vm block of environment"
            return spec.inventory.uuid
        }
    }

    @SpecMethod
    void useImage(String name) {
        preCreate {
            addDependency(name, ImageSpec.class)
        }

        image = {
            ImageSpec spec = findSpec(name, ImageSpec.class)
            assert spec != null: "cannot find image[$name], check the vm block of environment"
            return spec.inventory.uuid
        }
    }

    @SpecMethod
    void useRootDiskOffering(String name) {
        preCreate {
            addDependency(name, DiskOfferingSpec.class)
        }

        rootDiskOffering = {
            DiskOfferingSpec spec = findSpec(name, DiskOfferingSpec.class)
            assert spec != null: "cannot find useRootDiskOffering[$name], check the vm block of environment"
            return spec.inventory.uuid
        }
    }

    @SpecMethod
    void useCluster(String name) {
        preCreate {
            addDependency(name, ClusterSpec.class)
        }

        cluster = {
            ClusterSpec spec = findSpec(name, ClusterSpec.class)
            assert spec != null: "cannot find cluster[$name], check the vm block of environment"
            return spec.inventory.uuid
        }
    }

    @SpecMethod
    void useHost(String name) {
        preCreate {
            addDependency(name, HostSpec.class)
        }

        host = {
            HostSpec spec = findSpec(name, HostSpec.class)
            assert spec != null: "cannot find host[$name], check the vm block of environment"
            return spec.inventory.uuid
        }
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
    void useDiskOfferings(String... names) {
        names.each { String name ->
            preCreate {
                addDependency(name, DiskOfferingSpec.class)
            }
        }

        diskOfferings = {
            return names.collect { name ->
                DiskOfferingSpec spec = findSpec(name, DiskOfferingSpec.class)
                assert spec != null: "cannot find diskOffering[$name], check the vm block of environment"
                return spec.inventory.uuid
            }
        }
    }

    @SpecMethod
    void useL3Networks(String... names) {
        names.each { String name ->
            preCreate {
                addDependency(name, L3NetworkSpec.class)
            }
        }

        l3Networks = {
            return names.collect { name ->
                L3NetworkSpec spec = findSpec(name, L3NetworkSpec.class)
                assert spec != null: "cannot find l3Network[$name], check the vm block of environment"
                return spec.inventory.uuid
            }
        }
    }

    @SpecMethod
    void useDefaultL3Network(String name) {
        preCreate {
            addDependency(name, L3NetworkSpec.class)
        }

        defaultL3Network = {
            L3NetworkSpec spec = findSpec(name, L3NetworkSpec.class)
            assert spec != null: "cannot find defaultL3Network[$name], check the vm block of environment"
            return spec.inventory.uuid
        }
    }

    VmDiskSpec disk(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = VmDiskSpec.class) Closure c) {
        def diskSpec = new VmDiskSpec(envSpec)
        c.delegate = diskSpec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(diskSpec)
        disks << diskSpec
        return diskSpec
    }

    SpecID create(String uuid, String sessionId) {
        inventory = createVmInstance {
            delegate.resourceUuid = uuid
            delegate.name = name
            delegate.description = description
            delegate.sessionId = sessionId
            delegate.cpuNum = cpu
            delegate.memorySize = memorySize
            delegate.instanceOfferingUuid = instanceOffering()
            delegate.imageUuid = image()
            delegate.rootDiskOfferingUuid = rootDiskOffering()
            delegate.clusterUuid = cluster()
            delegate.hostUuid = host()
            delegate.primaryStorageUuidForRootVolume = primaryStorage()
            delegate.dataDiskOfferingUuids = diskOfferings()
            delegate.l3NetworkUuids = l3Networks()
            delegate.defaultL3NetworkUuid = defaultL3Network()
            delegate.systemTags = systemTags
            delegate.virtio = virtio

            if (!disks.isEmpty()) {
                List<DiskAO> diskAOs = []
                for (VmDiskSpec disk : disks) {
                    diskAOs << disk.toDiskAO()
                }
                if (diskAOs.every { (!it.boot) }) {
                    diskAOs.first().boot = true
                }
                delegate.diskAOs = diskAOs
            }
        }

        postCreate {
            inventory = queryVmInstance {
                conditions=["uuid=${inventory.uuid}".toString()]
            }[0]

            volumeToAttach.each { String volName ->
                DataVolumeSpec volume = findSpec(volName, DataVolumeSpec.class)
                def a = new AttachDataVolumeToVmAction()
                a.vmInstanceUuid = inventory.uuid
                a.volumeUuid = volume.inventory.uuid
                a.sessionId = sessionId
                def res = a.call()
                assert res.error == null : "AttachDataVolumeToVmAction failure: ${JSONObjectUtil.toJsonString(res.error)}"
            }
        }

        return id(name, inventory.uuid)
    }

    @SpecMethod
    void attachDataVolume(String...names) {
        names.each { String volName ->
            preCreate {
                addDependency(volName, DataVolumeSpec.class)
            }

            volumeToAttach.add(volName)
        }
    }

    @Override
    void delete(String sessionId) {
        if (inventory != null) {
            destroyVmInstance {
                delegate.uuid = inventory.uuid
                delegate.sessionId = sessionId
            }

            inventory = null
        }
    }

    void memoryGB(long memoryGB) {
        this.memorySize = SizeUnit.GIGABYTE.toByte(memoryGB)
    }
}
