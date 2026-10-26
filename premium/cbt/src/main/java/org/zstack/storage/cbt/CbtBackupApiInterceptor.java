package org.zstack.storage.cbt;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.cbt.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.image.APICreateDataVolumeTemplateFromVolumeMsg;
import org.zstack.header.image.APICreateRootVolumeTemplateFromRootVolumeMsg;
import org.zstack.header.vm.APIFlattenVmInstanceMsg;
import org.zstack.header.vm.APIMigrateVmMsg;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.APICreateVolumeSnapshotGroupMsg;
import org.zstack.header.volume.APICreateVolumeSnapshotMsg;
import org.zstack.header.volume.APIFlattenVolumeMsg;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.scheduler.APICreateSchedulerJobMsg;
import org.zstack.scheduler.SchedulerType;
import org.zstack.storage.migration.primary.APIPrimaryStorageMigrateVmMsg;
import org.zstack.storage.migration.primary.APIPrimaryStorageMigrateVolumeMsg;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Arrays;

import static org.zstack.core.Platform.operr;

@InterceptorForService("cbt")
public class CbtBackupApiInterceptor implements ApiMessageInterceptor, GlobalApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(CbtBackupApiInterceptor.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof CbtTaskMessage) {
            CbtTaskMessage cmsg = (CbtTaskMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, CbtBackupConstant.SERVICE_ID, cmsg.getCbtTaskUuid());
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIEnableCbtTaskMsg) {
            validate((APIEnableCbtTaskMsg) msg);
        }

        ErrorCode conflict = CbtTaskConflictChecker.checkTaskConflict(getConflictVmUuid(msg), null);
        if (conflict != null) {
            throw new ApiMessageInterceptionException(conflict);
        }

        setServiceId(msg);
        return msg;
    }

    private String getConflictVmUuid(APIMessage msg) {
        if (msg instanceof APIMigrateVmMsg) {
            return ((APIMigrateVmMsg) msg).getVmInstanceUuid();
        }
        if (msg instanceof APIPrimaryStorageMigrateVmMsg) {
            return ((APIPrimaryStorageMigrateVmMsg) msg).getVmInstanceUuid();
        }
        if (msg instanceof APIFlattenVmInstanceMsg) {
            APIFlattenVmInstanceMsg flatten = (APIFlattenVmInstanceMsg) msg;
            return flatten.isDryRun() ? null : flatten.getVmInstanceUuid();
        }
        if (msg instanceof APICreateSchedulerJobMsg) {
            APICreateSchedulerJobMsg scheduler = (APICreateSchedulerJobMsg) msg;
            if (!Arrays.asList(SchedulerType.VM_BACKUP, SchedulerType.ROOT_VOLUME_BACKUP,
                    SchedulerType.VOLUME_BACKUP, SchedulerType.VOLUME_SNAPSHOT,
                    SchedulerType.VOLUME_SNAPSHOT_GROUP).contains(scheduler.getType())) {
                return null;
            }
            String targetUuid = scheduler.getTargetResourceUuid();
            String vmUuid = getVolumeVmUuid(targetUuid);
            if (vmUuid != null) {
                return vmUuid;
            }
            return Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, targetUuid).isExists() ? targetUuid : null;
        }

        String volumeUuid;
        if (msg instanceof APICreateVolumeSnapshotMsg) {
            volumeUuid = ((APICreateVolumeSnapshotMsg) msg).getVolumeUuid();
        } else if (msg instanceof APICreateVolumeSnapshotGroupMsg) {
            volumeUuid = ((APICreateVolumeSnapshotGroupMsg) msg).getRootVolumeUuid();
        } else if (msg instanceof APICreateRootVolumeTemplateFromRootVolumeMsg) {
            volumeUuid = ((APICreateRootVolumeTemplateFromRootVolumeMsg) msg).getRootVolumeUuid();
        } else if (msg instanceof APICreateDataVolumeTemplateFromVolumeMsg) {
            volumeUuid = ((APICreateDataVolumeTemplateFromVolumeMsg) msg).getVolumeUuid();
        } else if (msg instanceof APIFlattenVolumeMsg) {
            APIFlattenVolumeMsg flatten = (APIFlattenVolumeMsg) msg;
            if (flatten.isDryRun()) {
                return null;
            }
            volumeUuid = flatten.getVolumeUuid();
        } else if (msg instanceof APIPrimaryStorageMigrateVolumeMsg) {
            volumeUuid = ((APIPrimaryStorageMigrateVolumeMsg) msg).getVolumeUuid();
        } else {
            return null;
        }
        return getVolumeVmUuid(volumeUuid);
    }

    private String getVolumeVmUuid(String volumeUuid) {
        return Q.New(VolumeVO.class)
                .eq(VolumeVO_.uuid, volumeUuid)
                .select(VolumeVO_.vmInstanceUuid)
                .findValue();
    }

    private void validate(APIEnableCbtTaskMsg msg) {
        CbtTaskVO taskVO = dbf.findByUuid(msg.getUuid(), CbtTaskVO.class);
        if (taskVO == null) {
            throw new ApiMessageInterceptionException(operr("Cbt task not found[uuid: %s]", msg.getUuid()));
        }

        if (taskVO.getStatus() == CbtTaskStatus.Running){
            throw new ApiMessageInterceptionException(operr("Cbt task[uuid: %s] has already enabled, please disable it.", msg.getUuid()));
        }
        final String vmUuid = getVmUuidByCbtTaskUuid(msg.getUuid());
        validateVmState(vmUuid, VmInstanceState.Running, "enable cbt task");

    }

    private String getVmUuidByCbtTaskUuid(String taskUuid) {
        String vmUuid = Q.New(CbtTaskResourceRefVO.class)
                .eq(CbtTaskResourceRefVO_.resourceType, VmInstanceVO.class.getSimpleName())
                .eq(CbtTaskResourceRefVO_.taskUuid, taskUuid)
                .select(CbtTaskResourceRefVO_.resourceUuid)
                .findValue();
        if (vmUuid == null) {
            throw new ApiMessageInterceptionException(operr("Cannot find vm for cbt task[uuid: %s].", taskUuid));
        }
        return vmUuid;
    }

    private void validateVmState(String vmUuid, VmInstanceState expectedState, String operation) {
        VmInstanceState state = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .select(VmInstanceVO_.state)
                .findValue();
        if (state != expectedState) {
            throw new ApiMessageInterceptionException(operr("Cannot %s for vm[uuid: %s], current vm state is %s, the expected vm state is %s.",
                    operation, vmUuid, state, expectedState));
        }
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return null;
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.FRONT;
    }
}
