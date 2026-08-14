package org.zstack.storage.backup;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.backup.BackupStorageConstant.AllocatorParams;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.err;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;


@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class DefaultBackupStorageAllocatorStrategy implements BackupStorageAllocatorStrategy {
    private FlowChain allocatorChain;

    @Autowired
    private ErrorFacade errf;

    public DefaultBackupStorageAllocatorStrategy(FlowChain allocatorChain) {
        this.allocatorChain = allocatorChain;
    }

    @Override
    public BackupStorageInventory allocate(BackupStorageAllocationSpec spec) throws BackupStorageException {
        return allocateAllCandidates(spec).get(0);
    }

    @Override
    public List<BackupStorageInventory> allocateAllCandidates(BackupStorageAllocationSpec spec) throws BackupStorageException {
        class Result {
            ErrorCode errorCode;
            List<BackupStorageVO> results;
        }

        final Result ret = new Result();
        allocatorChain.setName(String.format("allocate-backup-storage-msg-%s", spec.getAllocationMessage().getId()));
        allocatorChain.setData(map(e(AllocatorParams.SPEC, spec)));
        allocatorChain.done(new FlowDoneHandler(null) {
            @Override
            public void handle(Map data) {
                ret.results = (List<BackupStorageVO>) data.get(AllocatorParams.CANDIDATES);
            }
        }).error(new FlowErrorHandler(null) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                ret.errorCode = errCode;
            }
        }).start();

        if (ret.errorCode != null) {
            throw new BackupStorageException(
                    err(BackupStorageErrors.ALLOCATE_ERROR, "unable to allocate a backup storage").withCause(ret.errorCode));
        } else {
            Collections.shuffle(ret.results);
            return BackupStorageInventory.valueOf(ret.results);
        }
    }
}
