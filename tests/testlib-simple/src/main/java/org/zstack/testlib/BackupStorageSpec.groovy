package org.zstack.testlib

import org.zstack.sdk.BackupStorageInventory
import org.zstack.utils.data.SizeUnit

/**
 * Created by xing5 on 2017/2/15.
 */
abstract class BackupStorageSpec extends Spec {
    @SpecParam(required = true)
    String name
    @SpecParam
    String description
    @SpecParam(required = true)
    String url
    @SpecParam
    Long totalCapacity = SizeUnit.GIGABYTE.toByte(1000)
    @SpecParam
    Long availableCapacity = SizeUnit.GIGABYTE.toByte(1000)

    BackupStorageInventory inventory

    BackupStorageSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    ImageSpec image(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = ImageSpec.class) Closure c) {
        def i = new ImageSpec(envSpec)
        c.delegate = i
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(i)
        return i
    }

    @Override
    void delete(String sessionId) {
        if (inventory != null) {
            deleteBackupStorage {
                delegate.uuid = inventory.uuid
                delegate.sessionId = sessionId
            }

            inventory = null
        }
    }
}
