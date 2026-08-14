package org.zstack.testlib

import org.zstack.sdk.CephPrimaryStoragePoolInventory


/**
 * Created by xing5 on 2017/2/28.
 */
class CephPrimaryStoragePoolSpec extends Spec {
    @SpecParam(required = true)
    String poolName
    @SpecParam
    String description
    @SpecParam(required = true)
    String type

    CephPrimaryStoragePoolInventory inventory

    CephPrimaryStoragePoolSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    @Override
    void delete(String sessionId) {
        deleteCephPrimaryStoragePool {
            uuid = inventory.uuid
            delegate.sessionId = sessionId
        }
    }

    @Override
    SpecID create(String uuid, String sessionId) {

        inventory = addCephPrimaryStoragePool {
            delegate.poolName = poolName
            delegate.primaryStorageUuid = (parent as PrimaryStorageSpec).inventory.uuid
            delegate.description = description
            delegate.systemTags = systemTags
            delegate.userTags = userTags
            delegate.resourceUuid = uuid
            delegate.sessionId = sessionId
            delegate.type = type
            delegate.isCreate = true
        }

        return id(poolName, inventory.uuid)
    }
}
