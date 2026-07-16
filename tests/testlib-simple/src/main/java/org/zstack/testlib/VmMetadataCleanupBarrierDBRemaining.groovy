package org.zstack.testlib

import org.zstack.header.vm.metadata.VmMetadataCleanupBarrierState
import org.zstack.header.vm.metadata.VmMetadataCleanupBarrierVO

class VmMetadataCleanupBarrierDBRemaining extends AllowedDBRemaining {
    @Override
    void remaining() {
        table {
            tableVOClass = VmMetadataCleanupBarrierVO.class
            checker = { List<VmMetadataCleanupBarrierVO> barriers ->
                return barriers.findAll { VmMetadataCleanupBarrierVO barrier ->
                    barrier.id != VmMetadataCleanupBarrierVO.SINGLETON_ID ||
                            barrier.state != VmMetadataCleanupBarrierState.Idle ||
                            barrier.operationUuid != null ||
                            barrier.managementNodeUuid != null ||
                            barrier.leaseExpireDate != null ||
                            barrier.generation < 0
                }
            }
        }
    }
}
