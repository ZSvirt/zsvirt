package org.zstack.storage.backup;

/**
 * Created by MaJin on 2020/7/30.
 */

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.Q;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.primary.PrimaryStorageType;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;

import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;
/**
 * Created by MaJin on 2020/7/30.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class BackupStoragePrimaryStorageAllocatorFlow extends NoRollbackFlow {

    @Override
    public void run(FlowTrigger trigger, Map data) {
        BackupStorageAllocationSpec spec = (BackupStorageAllocationSpec) data.get(BackupStorageConstant.AllocatorParams.SPEC);
        if (spec.getRequiredPrimaryStorageUuid() == null) {
            trigger.next();
            return;
        }

        String psTypeName = Q.New(PrimaryStorageVO.class).select(PrimaryStorageVO_.type)
                .eq(PrimaryStorageVO_.uuid, spec.getRequiredPrimaryStorageUuid())
                .findValue();

        PrimaryStorageType psType = PrimaryStorageType.valueOf(psTypeName);
        List<String> supportBsUuids = psType.findBackupStorage(spec.getRequiredPrimaryStorageUuid());

        List<BackupStorageVO> candidates =  (List<BackupStorageVO>)data.get(BackupStorageConstant.AllocatorParams.CANDIDATES);
        candidates.removeIf(it -> !supportBsUuids.contains(it.getUuid()));

        if (candidates.isEmpty()) {
            trigger.fail(operr("required primary storage[uuid:%s, type:%s] could not support any backup storage.",
                    spec.getRequiredPrimaryStorageUuid(), psTypeName));
            return;
        }

        data.put(BackupStorageConstant.AllocatorParams.CANDIDATES, candidates);
        trigger.next();
    }
}
