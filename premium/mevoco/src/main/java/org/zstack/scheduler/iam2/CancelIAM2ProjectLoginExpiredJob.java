package org.zstack.scheduler.iam2;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.Message;
import org.zstack.header.scheduler.CreateSchedulerJobDescMsg;
import org.zstack.scheduler.AbstractSchedulerJob;
import org.zstack.scheduler.SchedulerType;
import org.zstack.scheduler.autoscalinggroup.AutoScalingGroupJob;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;


/**
 * @Author: DaoDao
 * @Date: 2021/8/23
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CancelIAM2ProjectLoginExpiredJob extends AbstractSchedulerJob {
    private static final CLogger logger = Utils.getLogger(CancelIAM2ProjectLoginExpiredJob.class);
    @Autowired
    private transient PluginRegistry pluginRgty;
    private String state = "LoginExpired";

    public CancelIAM2ProjectLoginExpiredJob(CreateSchedulerJobDescMsg msg) {
        super(msg);
    }

    public CancelIAM2ProjectLoginExpiredJob() {
        super();
    }


    @Override
    public Object buildRequest() {
        for (CancelIAM2ProjectLoginExpiredExtensionPoint ext : pluginRgty.getExtensionList(CancelIAM2ProjectLoginExpiredExtensionPoint.class)) {
            Message message = ext.buildRequest(getTargetResourceUuid(), state);
            if(message != null) {
                return message;
            }
        }

        return null;
    }

    @Override
    public void execute(Object request, ReturnValueCompletion completion) {
        logger.info(String.format("cancle IAM2Project[%s] Login Expired", getTargetResourceUuid()));
        for (CancelIAM2ProjectLoginExpiredExtensionPoint ext : pluginRgty.getExtensionList(CancelIAM2ProjectLoginExpiredExtensionPoint.class)) {
            ext.cancelIAM2ProjectLoginExpired(getTargetResourceUuid(), getTriggerUuid(), completion);
        }
    }

    @Override
    public String getType() {
        return SchedulerType.CANCEL_IAM2_PROJECT_LOGIN_EXPIRED;
    }

    @Override
    public ErrorCode allowStateChange() {
        for (CancelIAM2ProjectLoginExpiredExtensionPoint ext : pluginRgty.getExtensionList(CancelIAM2ProjectLoginExpiredExtensionPoint.class)) {
            ErrorCode err = ext.allowStateChange(getTargetResourceUuid());
            if (err != null) {
                return err;
            }
        }
        return null;
    }
}
