package org.zstack.testlib

import org.zstack.sdk.HostInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.storage.primary.local.LocalStorageConstants

/**
 * Created by mingjian.deng on 2018/1/3.
 */
class DataVolumeSpec extends Spec implements HasSession {
    @SpecParam
    String name = "data-volume"
    @SpecParam
    String description
    private Closure diskOfferings = {}
    private Closure primaryStorage = {}
    private Closure host = {}

    VolumeInventory inventory

    DataVolumeSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    SpecID create(String uuid, String sessionId) {
        inventory = createDataVolume {
            delegate.resourceUuid = uuid
            delegate.name = name
            delegate.primaryStorageUuid = primaryStorage()
            delegate.diskOfferingUuid = diskOfferings()
            delegate.description = description
            delegate.systemTags = host() ? ["localStorage::hostUuid::${host()}".toString()] : null
            delegate.sessionId = sessionId
        }

        postCreate {
            inventory = queryVolume {
                conditions=["uuid=${inventory.uuid}".toString()]
            }[0]
        }

        return id(name, inventory.uuid)
    }

    @SpecMethod
    void useDiskOffering(String name) {
        preCreate {
            addDependency(name, DiskOfferingSpec.class)
        }

        diskOfferings = {
            DiskOfferingSpec spec = findSpec(name, DiskOfferingSpec.class)
            assert spec != null: "cannot find useDiskOffering[$name], check the vm block of environment"
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
            assert spec != null: "cannot find usePrimaryStorage[$name], check the primaryStorage block of environment"
            return spec.inventory.uuid
        }
    }

    @SpecMethod
    void useHost(String name){
        preCreate {
            addDependency(name, HostSpec.class)
        }

        host = {
            HostSpec spec = findSpec(name, HostSpec.class)
            assert spec != null: "cannot find useHost[$name], check the host block of environment"
            assert primaryStorage() != null

            HostInventory hostInventory = spec.inventory
            List primaryStorageInventories = queryPrimaryStorage {
                conditions = ["attachedClusterUuids=${hostInventory.clusterUuid}", "type=${LocalStorageConstants.LOCAL_STORAGE_TYPE}".toString(), "uuid=${primaryStorage()}".toString()]
            }
            assert primaryStorageInventories.size() > 0: "cannot find useHost[$name], check whether the host and the localPrimaryStorage blocks of environment are in the same cluster"

            return hostInventory.uuid
        }
    }

    @Override
    void delete(String sessionId) {
        if (inventory != null) {
            deleteDataVolume {
                delegate.uuid = inventory.uuid
                delegate.sessionId = sessionId
            }

            inventory = null
        }
    }
}
