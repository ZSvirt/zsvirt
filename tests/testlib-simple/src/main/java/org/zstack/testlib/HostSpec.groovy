package org.zstack.testlib

import org.zstack.sdk.HostInventory
import org.zstack.utils.data.SizeUnit

/**
 * Created by xing5 on 2017/2/12.
 */
abstract class HostSpec extends Spec {
    @SpecParam(required = true)
    String name
    @SpecParam
    String description
    @SpecParam
    String managementIp = "127.0.0.1"
    @SpecParam
    Long totalMem = SizeUnit.GIGABYTE.toByte(32)
    @SpecParam
    Long usedMem = 0
    @SpecParam
    Integer totalCpu = 32
    @SpecParam
    Integer usedCpu = 0
    @SpecParam
    Integer cpuSockets = 2
    @SpecParam
    Integer cpuCores = 16

    HostInventory inventory

    HostSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    @Override
    void delete(String sessionId) {
        if (inventory != null) {
            deleteHost {
                delegate.uuid = inventory.uuid
                delegate.sessionId = sessionId
            }

            inventory = null
        }
    }
}
