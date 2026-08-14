package org.zstack.storage.backup;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.backup.BackupStorageConstant.AllocatorParams;

import static org.zstack.core.Platform.operr;

import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Map;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class BackupStorageMainAllocatorFlow extends NoRollbackFlow {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    private class Result {
        String error;
        List<BackupStorageVO> result;
    }

    @Transactional(readOnly = true)
    private Result allocate(Map data) {
        BackupStorageAllocationSpec spec = (BackupStorageAllocationSpec) data.get(AllocatorParams.SPEC);
        TypedQuery<BackupStorageVO> query = null;
        String error;
        if (spec.getRequiredBackupStorageUuid() != null && spec.getRequiredZoneUuid() != null) {
            String sql = "select bs from BackupStorageVO bs, BackupStorageZoneRefVO ref where ref.backupStorageUuid = bs.uuid and ref.zoneUuid = :zoneUuid and bs.uuid = :uuid and bs.availableCapacity > :size and bs.status = :status and bs.state = :state";
            query = dbf.getEntityManager().createQuery(sql, BackupStorageVO.class);
            query.setParameter("uuid", spec.getRequiredBackupStorageUuid());
            query.setParameter("zoneUuid", spec.getRequiredZoneUuid());
            query.setParameter("size", spec.getSize());
            query.setParameter("status", BackupStorageStatus.Connected);
            query.setParameter("state", BackupStorageState.Enabled);
            error = String.format("backup storage[uuid:%s] can not satisfy one or more conditions[available capacity > %s bytes, status = %s, state = %s], or it doesn't attach to zone[uuid:%s]",
                    spec.getRequiredBackupStorageUuid(), spec.getSize(), BackupStorageStatus.Connected, BackupStorageState.Enabled, spec.getRequiredZoneUuid());
        } else if (spec.getRequiredBackupStorageUuid() != null) {
            String sql = "select bs from BackupStorageVO bs where bs.uuid = :uuid and bs.availableCapacity > :size and bs.status = :status and bs.state = :state";
            query = dbf.getEntityManager().createQuery(sql, BackupStorageVO.class);
            query.setParameter("uuid", spec.getRequiredBackupStorageUuid());
            query.setParameter("size", spec.getSize());
            query.setParameter("status", BackupStorageStatus.Connected);
            query.setParameter("state", BackupStorageState.Enabled);
            error = String.format("backup storage[uuid:%s] can not satisfy one or more conditions[available capacity > %s bytes, status = %s, state = %s]",
                    spec.getRequiredBackupStorageUuid(), spec.getSize(), BackupStorageStatus.Connected, BackupStorageState.Enabled);
        } else if (spec.getRequiredZoneUuid() != null) {
            String sql = "select bs from BackupStorageVO bs, BackupStorageZoneRefVO ref where bs.availableCapacity > :size and bs.status = :status and bs.state = :state and bs.uuid = ref.backupStorageUuid and ref.zoneUuid = :zoneUuid";
            query = dbf.getEntityManager().createQuery(sql, BackupStorageVO.class);
            query.setParameter("size", spec.getSize());
            query.setParameter("status", BackupStorageStatus.Connected);
            query.setParameter("state", BackupStorageState.Enabled);
            query.setParameter("zoneUuid", spec.getRequiredZoneUuid());
            error = String.format("no backup storage that satisfies conditions[available capacity > %s bytes, status = %s, state = %s, attached zone uuid=%s] found",
                    spec.getSize(), BackupStorageStatus.Connected, BackupStorageState.Enabled, spec.getRequiredZoneUuid());
        } else {
            String sql = "select bs from BackupStorageVO bs where bs.availableCapacity > :size and bs.status = :status and bs.state = :state";
            query = dbf.getEntityManager().createQuery(sql, BackupStorageVO.class);
            query.setParameter("size", spec.getSize());
            query.setParameter("status", BackupStorageStatus.Connected);
            query.setParameter("state", BackupStorageState.Enabled);
            error = String.format("no backup storage that satisfies conditions[available capacity > %s bytes, status = %s, state = %s] found",
                    spec.getSize(), BackupStorageStatus.Connected, BackupStorageState.Enabled);
        }

        Result ret = new Result();
        ret.result = query.getResultList();
        ret.error = error;
        return ret;
    }

    @Override
    public void run(FlowTrigger trigger, Map data) {
        Result ret = allocate(data);
        if (ret.result.isEmpty()) {
            throw new OperationFailureException(operr("%s", ret.error));
        }

        data.put(AllocatorParams.CANDIDATES, ret.result);
        trigger.next();
    }
}
