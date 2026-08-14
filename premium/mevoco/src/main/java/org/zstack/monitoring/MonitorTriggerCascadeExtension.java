package org.zstack.monitoring;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmDeletionStruct;
import org.zstack.header.vm.VmInstanceVO;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

/**
 * Created by xing5 on 2017/8/8.
 */
public class MonitorTriggerCascadeExtension extends AbstractAsyncCascadeExtension {
    @Autowired
    private CloudBus bus;

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_CHECK_CODE)) {
            handleDeletionCheck(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_DELETE_CODE, CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            handleDeletion(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_CLEANUP_CODE)) {
            handleDeletionCleanup(action, completion);
        } else {
            completion.success();
        }
    }

    private void handleDeletionCleanup(CascadeAction action, Completion completion) {
        completion.success();
    }

    private void handleDeletion(CascadeAction action, Completion completion) {
        List<String> monitorTriggerUuids = null;

        if (HostVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<String> hostUuids = ((List<HostInventory>)action.getParentIssuerContext()).stream().map(HostInventory::getUuid).collect(Collectors.toList());
            monitorTriggerUuids = Q.New(MonitorTriggerVO.class).select(MonitorTriggerVO_.uuid).in(MonitorTriggerVO_.targetResourceUuid, hostUuids).listValues();
        } else if (VmInstanceVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<String> vmUuids = ((List<VmDeletionStruct>)action.getParentIssuerContext()).stream().map(v -> v.getInventory().getUuid()).collect(Collectors.toList());
            monitorTriggerUuids = Q.New(MonitorTriggerVO.class).select(MonitorTriggerVO_.uuid).in(MonitorTriggerVO_.targetResourceUuid, vmUuids).listValues();
        }

        if (monitorTriggerUuids == null || monitorTriggerUuids.isEmpty()) {
            completion.success();
            return;
        }

        SQL.New(MonitorTriggerVO.class).in(MonitorTriggerVO_.uuid, monitorTriggerUuids).hardDelete();
        List<MonitorTriggerDeletionMsg> msgs = monitorTriggerUuids.stream().map(uuid -> {
            MonitorTriggerDeletionMsg msg = new MonitorTriggerDeletionMsg();
            msg.setUuid(uuid);
            bus.makeTargetServiceIdByResourceUuid(msg, MonitorConstants.SERVICE_ID, uuid);
            return msg;
        }).collect(Collectors.toList());

        new While<>(msgs).all((msg, c) -> bus.send(msg, new CloudBusCallBack(c) {
            @Override
            public void run(MessageReply reply) {
                c.done();
            }
        })).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.success();
            }
        });
    }

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
        completion.success();
    }

    @Override
    public List<String> getEdgeNames() {
        return asList(VmInstanceVO.class.getSimpleName(), HostVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return MonitorTriggerVO.class.getSimpleName();
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        return null;
    }
}
