package org.zstack.testlib

import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.utils.data.SizeUnit

/**
 * Created by xing5 on 2017/2/13.
 */
abstract class PrimaryStorageSpec extends Spec {
    @SpecParam(required = true)
    String name
    @SpecParam
    String description
    @SpecParam(required = true)
    String url
    @SpecParam
    Long totalCapacity = SizeUnit.TERABYTE.toByte(100)
    @SpecParam
    Long availableCapacity = SizeUnit.TERABYTE.toByte(100)

    PrimaryStorageInventory inventory

    PrimaryStorageSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    @Override
    void delete(String sessionId) {
        if (inventory != null) {
            deletePrimaryStorage {
                delegate.uuid = inventory.uuid
                delegate.sessionId = sessionId
            }

            inventory = null
        }
    }
}
