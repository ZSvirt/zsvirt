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
        final String vmUuid = Q.New(CbtTaskResourceRefVO.class)
                .eq(CbtTaskResourceRefVO_.resourceType, VmInstanceVO.class.getSimpleName())
                .eq(CbtTaskResourceRefVO_.taskUuid, msg.getUuid())
                .select(CbtTaskResourceRefVO_.resourceUuid)
                .findValue();

        VmInstanceState state = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .select(VmInstanceVO_.state)
                .findValue();
        if (state != VmInstanceState.Running) {
            throw new ApiMessageInterceptionException(operr("Unexpected vm[uuid: %s] state: %s, the expected vm state is Running.", vmUuid, state));
        }

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

    @Override
    public List<Class> getMessageClassToIntercept() {
        return null;
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.FRONT;
    }
}
