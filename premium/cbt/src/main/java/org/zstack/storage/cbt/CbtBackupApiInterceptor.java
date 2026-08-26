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
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.scheduler.SchedulerFacade;
import org.zstack.scheduler.SchedulerType;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.operr;

@InterceptorForService("cbt")
public class CbtBackupApiInterceptor implements ApiMessageInterceptor, GlobalApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(CbtBackupApiInterceptor.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private SchedulerFacade schedulerFacade;

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
        } else if (msg instanceof APIDisableCbtTaskMsg) {
            validate((APIDisableCbtTaskMsg) msg);
        } else if (msg instanceof APIExportNbdVolumesMsg) {
            validate((APIExportNbdVolumesMsg) msg);
        } else if (msg instanceof APIUnexportNbdVolumesMsg) {
            validate((APIUnexportNbdVolumesMsg) msg);
        }

        setServiceId(msg);
        return msg;
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

        List<String> resourceUuids = Q.New(VolumeVO.class)
                .eq(VolumeVO_.vmInstanceUuid, vmUuid)
                .select(VolumeVO_.uuid)
                .listValues();
        resourceUuids.add(vmUuid);

        for (String resourceUuid : resourceUuids) {
            List<String> resourceSchedulerJobTypes = schedulerFacade.getResourceSchedulerJobTypes(resourceUuid);
            if (CollectionUtils.isEmpty(resourceSchedulerJobTypes)) {
                continue;
            }
            resourceSchedulerJobTypes.retainAll(asList(SchedulerType.VM_BACKUP, SchedulerType.ROOT_VOLUME_BACKUP, SchedulerType.VOLUME_BACKUP));
            if (!CollectionUtils.isEmpty(resourceSchedulerJobTypes)) {
                throw new ApiMessageInterceptionException(operr("The vm[uuid: %s] has already created a backup job, cannot enable the cbt task at the same time.", vmUuid));
            }
        }
    }

    private void validate(APIDisableCbtTaskMsg msg) {
        CbtTaskVO taskVO = dbf.findByUuid(msg.getUuid(), CbtTaskVO.class);
        if (taskVO == null) {
            throw new ApiMessageInterceptionException(operr("Cbt task not found[uuid: %s]", msg.getUuid()));
        }

        String vmUuid = getVmUuidByCbtTaskUuid(msg.getUuid());
        validateVmState(vmUuid, VmInstanceState.Running, "disable cbt task");
    }

    private void validate(APIExportNbdVolumesMsg msg) {
        validateVmState(msg.getVmInstanceUuid(), VmInstanceState.Stopped, "export NBD volumes");
    }

    private void validate(APIUnexportNbdVolumesMsg msg) {
        validateVmState(msg.getVmInstanceUuid(), VmInstanceState.Stopped, "unexport NBD volumes");
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
