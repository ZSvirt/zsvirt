package org.zstack.storage.cbt;

import org.zstack.core.db.Q;
import org.zstack.header.cbt.CbtTaskResourceRefVO;
import org.zstack.header.cbt.CbtTaskResourceRefVO_;
import org.zstack.header.cbt.CbtTaskStatus;
import org.zstack.header.cbt.CbtTaskVO;
import org.zstack.header.cbt.CbtTaskVO_;
import org.zstack.header.cbt.VolumeCbtBackupRecordVO;
import org.zstack.header.cbt.VolumeCbtBackupRecordVO_;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceVO;

import java.util.Collections;
import java.util.List;

import static org.zstack.core.Platform.operr;

final class CbtTaskConflictChecker {
    private CbtTaskConflictChecker() {
    }

    static List<String> getTaskUuids(String vmUuid) {
        if (vmUuid == null) {
            return Collections.emptyList();
        }
        return Q.New(CbtTaskResourceRefVO.class)
                .eq(CbtTaskResourceRefVO_.resourceType, VmInstanceVO.class.getSimpleName())
                .eq(CbtTaskResourceRefVO_.resourceUuid, vmUuid)
                .select(CbtTaskResourceRefVO_.taskUuid)
                .listValues();
    }

    static ErrorCode checkTaskConflict(String vmUuid, String currentTaskUuid) {
        List<String> taskUuids = getTaskUuids(vmUuid);
        if (currentTaskUuid != null) {
            taskUuids.removeIf(currentTaskUuid::equals);
        }
        if (taskUuids.isEmpty()) {
            return null;
        }

        String runningTaskUuid = Q.New(CbtTaskVO.class)
                .in(CbtTaskVO_.uuid, taskUuids)
                .eq(CbtTaskVO_.status, CbtTaskStatus.Running)
                .select(CbtTaskVO_.uuid)
                .findValue();
        if (runningTaskUuid != null) {
            return operr("VM[uuid: %s] has a running CBT task[uuid: %s], please disable it before performing this operation.",
                    vmUuid, runningTaskUuid);
        }

        List<String> failedTaskUuids = Q.New(CbtTaskVO.class)
                .in(CbtTaskVO_.uuid, taskUuids)
                .eq(CbtTaskVO_.status, CbtTaskStatus.Failed)
                .select(CbtTaskVO_.uuid)
                .listValues();
        if (failedTaskUuids.isEmpty()) {
            return null;
        }
        String pendingTaskUuid = Q.New(VolumeCbtBackupRecordVO.class)
                .in(VolumeCbtBackupRecordVO_.taskUuid, failedTaskUuids)
                .notNull(VolumeCbtBackupRecordVO_.cleanupAttemptUuid)
                .select(VolumeCbtBackupRecordVO_.taskUuid)
                .limit(1)
                .findValue();
        return pendingTaskUuid == null ? null : operr(
                "VM[uuid: %s] has a failed CBT task[uuid: %s] with pending runtime cleanup, please complete cleanup before performing this operation.",
                vmUuid, pendingTaskUuid);
    }
}
