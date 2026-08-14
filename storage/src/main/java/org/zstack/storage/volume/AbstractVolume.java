package org.zstack.storage.volume;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.image.APICreateDataVolumeTemplateFromVolumeMsg;
import org.zstack.header.image.APICreateRootVolumeTemplateFromRootVolumeMsg;
import org.zstack.header.message.Message;
import org.zstack.header.storage.backup.VolumeBackupOverlayMsg;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.vm.*;
import org.zstack.header.volume.*;
import org.zstack.utils.message.OperationChecker;

import java.util.Set;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public abstract class AbstractVolume {
    protected static OperationChecker allowedOperations = new OperationChecker(true);
    protected static OperationChecker forbiddenOperations = new OperationChecker(true);
    protected static OperationChecker stateChangeChecker = new OperationChecker(false);

    @Autowired
    protected ErrorFacade errf;

    static {
        allowedOperations.addState(VolumeStatus.Ready,
                ChangeVolumeStatusMsg.class.getName(),
                VolumeDeletionMsg.class.getName(),
                DeleteVolumeMsg.class.getName(),
                APICreateVolumeSnapshotMsg.class.getName(),
                APIDeleteVolumeSnapshotMsg.class.getName(),
                APIDeleteDataVolumeMsg.class.getName(),
                APIBatchDeleteVolumeSnapshotMsg.class.getName(),
                APICreateRootVolumeTemplateFromRootVolumeMsg.class.getName(),
                APICreateDataVolumeTemplateFromVolumeMsg.class.getName(),
                VolumeTemplateOverlayMsg.class.getName(),
                VolumeCreateSnapshotMsg.class.getName(),
                CreateVolumesSnapshotOverlayVolumeMsg.class.getName(),
                ReimageVolumeOverlayMsg.class.getName(),
                VolumeSnapshotDeletionMsg.class.getName(),
                CreateVolumeSnapshotGroupMsg.class.getName(),
                VolumeBackupOverlayMsg.class.getName()
                );

        allowedOperations.addState(VolumeStatus.Migrating,
                ChangeVolumeStatusMsg.class.getName()
        );

        allowedOperations.addState(VolumeStatus.Deleted,
                VolumeDeletionMsg.class.getName(),
                DeleteVolumeMsg.class.getName(),
                VolumeSnapshotDeletionMsg.class.getName(),
                APIDeleteVolumeSnapshotMsg.class.getName(),
                APIDeleteDataVolumeMsg.class.getName(),
                APIBatchDeleteVolumeSnapshotMsg.class.getName()
        );

        allowedOperations.addState(VolumeStatus.Creating,
                VolumeDeletionMsg.class.getName(),
                DeleteVolumeMsg.class.getName()
        );

        allowedOperations.addState(VolumeStatus.NotInstantiated,
                VolumeDeletionMsg.class.getName(),
                DeleteVolumeMsg.class.getName()
        );

        forbiddenOperations.addState(VolumeStatus.Migrating,
                VolumeTemplateOverlayMsg.class.getName(),
                VolumeBackupOverlayMsg.class.getName(),
                CreateVolumeSnapshotGroupMsg.class.getName(),
                VolumeCreateSnapshotMsg.class.getName(),
                APIDeleteVolumeSnapshotMsg.class.getName(),
                VolumeSnapshotDeletionMsg.class.getName(),
                VolumeSnapshotDeletionOverlayVolumeMsg.class.getName(),
                VolumeSnapshotOverlayMsg.class.getName());
    }

    public static Set<String> getAllowedStatesForOperation(Class<? extends Message> clz) {
        return allowedOperations.getStatesForOperation(clz.getName());
    }

    private ErrorCode validateOperationByState(OperationChecker checker, Message msg, VolumeStatus currentState, Enum errorCode) {
        if (!checker.isOperationForbidden(msg.getMessageName(), currentState.toString())) {
            return null;
        }

        ErrorCode cause = Platform.err(VmErrors.NOT_IN_CORRECT_STATE, "current volume state[%s] doesn't allow to proceed message[%s]", currentState,
                msg.getMessageName());
        if (errorCode != null) {
            return Platform.err(errorCode, cause.getDetails()).withCause(cause);
        }
        return cause;
    }

    public ErrorCode validateOperationByState(Message msg, VolumeStatus currentState, Enum errorCode) {
        return validateOperationByState(forbiddenOperations, msg, currentState, errorCode);
    }

}
