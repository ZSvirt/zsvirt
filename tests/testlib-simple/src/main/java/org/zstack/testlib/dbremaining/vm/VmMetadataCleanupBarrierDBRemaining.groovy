package org.zstack.testlib.dbremaining.vm

import org.zstack.header.vm.metadata.VmMetadataCleanupBarrierState
import org.zstack.header.vm.metadata.VmMetadataCleanupBarrierVO
import org.zstack.testlib.dbremaining.AllowedDBRemaining

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
