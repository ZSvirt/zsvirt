package org.zstack.zwatch.monitorgroup;

import java.util.Arrays;
import java.util.List;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.message.APIMessage;
import org.zstack.identity.AccountManager;
import org.zstack.zwatch.ZWatchGlobalConfig;
import org.zstack.zwatch.monitorgroup.api.*;
import org.zstack.zwatch.monitorgroup.entity.MonitorGroupInstanceVO;
import org.zstack.zwatch.monitorgroup.entity.MonitorGroupInstanceVO_;
import org.zstack.zwatch.monitorgroup.entity.MonitorGroupTemplateRefVO;
import org.zstack.zwatch.monitorgroup.entity.MonitorGroupTemplateRefVO_;

import static org.zstack.core.Platform.argerr;

public class MonitorGroupApiInterceptor implements GlobalApiMessageInterceptor {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private AccountManager acntMgr;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddInstanceToMonitorGroupMsg) {
            validate((APIAddInstanceToMonitorGroupMsg) msg);
        } else if (msg instanceof APIRemoveInstanceFromMonitorGroupMsg) {
            validate((APIRemoveInstanceFromMonitorGroupMsg) msg);
        } else if (msg instanceof APIRevokeMonitorTemplateFromMonitorGroupMsg) {
            validate((APIRevokeMonitorTemplateFromMonitorGroupMsg) msg);
        }

        return msg;
    }

    private void validate(APIAddInstanceToMonitorGroupMsg msg) {
        long instanceNum = Q.New(MonitorGroupInstanceVO.class)
                .eq(MonitorGroupInstanceVO_.groupUuid, msg.getGroupUuid())
                .count();
        long max = ZWatchGlobalConfig.MONITOR_GROUP_MAXIMUM_INSTANCE_NUM.value(Long.class);
        if (instanceNum >= max) {
            throw new ApiMessageInterceptionException(argerr("The instance in the group has reached the maximum limit"));
        }

        boolean exists = Q.New(MonitorGroupInstanceVO.class)
                .eq(MonitorGroupInstanceVO_.groupUuid, msg.getGroupUuid())
                .eq(MonitorGroupInstanceVO_.instanceUuid, msg.getInstanceUuid())
                .isExists();
        if (exists) {
            throw new ApiMessageInterceptionException(argerr("the instance[%s] is already in the group", msg.getInstanceUuid()));
        }

    }

    private void validate(APIRemoveInstanceFromMonitorGroupMsg msg) {
        boolean exists = Q.New(MonitorGroupInstanceVO.class)
                .eq(MonitorGroupInstanceVO_.groupUuid, msg.getGroupUuid())
                .eq(MonitorGroupInstanceVO_.instanceUuid, msg.getInstanceUuid())
                .isExists();
        if (!exists) {
            throw new ApiMessageInterceptionException(argerr("instance[%s] is not in the group", msg.getInstanceUuid()));
        }
    }

    private void validate(APIRevokeMonitorTemplateFromMonitorGroupMsg msg) {
        boolean exists = Q.New(MonitorGroupTemplateRefVO.class)
                .eq(MonitorGroupTemplateRefVO_.groupUuid, msg.getGroupUuid())
                .eq(MonitorGroupTemplateRefVO_.templateUuid, msg.getTemplateUuid())
                .isExists();
        if (!exists) {
            throw new ApiMessageInterceptionException(argerr("The monitorGroup[%s] does not have an monitorTemplate applied", msg.getGroupUuid()));
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List<Class> getMessageClassToIntercept() {
        return Arrays.asList(
            APIAddInstanceToMonitorGroupMsg.class,
            APIRemoveInstanceFromMonitorGroupMsg.class,
            APIRevokeMonitorTemplateFromMonitorGroupMsg.class
        );
    }

    @Override
    public GlobalApiMessageInterceptor.InterceptorPosition getPosition() {
        return GlobalApiMessageInterceptor.InterceptorPosition.DEFAULT;
    }

}
