package org.zstack.externalbackup;

import org.zstack.core.progress.TaskTracker;
import org.zstack.header.errorcode.ErrorCode;

/**
 * Created by MaJin on 2020/4/8.
 */
public class ExternalBackupRecoverTracker extends TaskTracker {
    protected static final String TASK_NAME = "ExternalBackupRecover";

    protected static final String RESOURCE_TYPE = "recoverResourceType";
    protected static final String RESOURCE_UUID = "recoverResourceUuid";
    protected static final String PROCESS = "resourceRecoverProcess";

    public enum Process {
        success,
        failure,
        starting,
    }

    public void startRecover(String resourceUuid, String resourceType) {
        String info = String.format("Restoring %s[uuid:%s].", getReadableType(resourceType), resourceUuid);
        refreshParam(resourceUuid, resourceType, Process.starting, null);
        track(info);
    }

    public void completeRecover(String resourceUuid, String resourceType, ErrorCode error) {
        if (error == null) {
            refreshParam(resourceUuid, resourceType, Process.success, null);
            track(String.format("Successfully restored %s[uuid:%s].", getReadableType(resourceType), resourceUuid));
        } else {
            refreshParam(resourceUuid, resourceType, Process.failure, error);
            track(String.format("Fail to restored %s[uuid:%s]. Reason: %s",
                    getReadableType(resourceType), resourceUuid, error.getReadableDetails()));
        }
    }

    private static String getReadableType(String type) {
        return type.endsWith("O") ? type.substring(0, type.length() - 2).toLowerCase() : type.toLowerCase();
    }

    void start() {
        refreshParam(null, null, Process.starting, null);
        track("Restoring data from backup.");
    }

    void success() {
        refreshParam(null, null, Process.success, null);
        track("Successfully restored data from backup.");
    }

    void fail(ErrorCode error) {
        refreshParam(null, null, Process.failure, error);
        track("Failed to restore data from backup. Reason: " + error.getReadableDetails());
    }

    protected void refreshParam(String resourceUuid, String resourceType, Process process, ErrorCode error) {
        param(RESOURCE_TYPE, resourceUuid);
        param(RESOURCE_UUID, resourceType);
        param(PROCESS, process);
        param(PARAM_ERROR, error);
    }

    public ExternalBackupRecoverTracker(String backupUuid) {
        super(backupUuid);
    }

    @Override
    protected String getResourceType() {
        return ExternalBackupVO.class.getSimpleName();
    }

    @Override
    protected String getTaskName() {
        return TASK_NAME;
    }
}
