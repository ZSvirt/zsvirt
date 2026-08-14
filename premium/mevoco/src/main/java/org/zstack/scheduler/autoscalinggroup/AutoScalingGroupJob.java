package org.zstack.scheduler.autoscalinggroup;


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
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * @author qiuyu.zhang
 * @Package org.zstack.scheduler.autoscalinggroup
 * @date 2020/12/8 3:08 PM
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AutoScalingGroupJob extends AbstractSchedulerJob {
    private static final CLogger logger = Utils.getLogger(AutoScalingGroupJob.class);
    @Autowired
    private transient PluginRegistry pluginRgty;

    public AutoScalingGroupJob(CreateSchedulerJobDescMsg msg) {
        super(msg);
    }

    public AutoScalingGroupJob() {
        super();
    }

    @Override
    public Object buildRequest() {
        Message msg = null;
        for (TakeAutoScalingSchedulerJobExtensionPoint ext : pluginRgty.getExtensionList(TakeAutoScalingSchedulerJobExtensionPoint.class)) {
            msg = ext.buildRequest(getUuid(), getTargetResourceUuid());
        }
        return msg;
    }

    @Override
    public void execute(Object msg, ReturnValueCompletion completion) {
        logger.debug(String.format("run scheduler for job: AutoScalingGroupJob; SchedulerJob uuid is %s", getTargetResourceUuid()));

        for (TakeAutoScalingSchedulerJobExtensionPoint ext : pluginRgty.getExtensionList(TakeAutoScalingSchedulerJobExtensionPoint.class)) {
            ext.takeAutoScalingSchedulerJob(getUuid(), getTargetResourceUuid(), completion);

        }
    }

    @Override
    public ErrorCode allowStateChange() {
        ErrorCode code = null;
        for (TakeAutoScalingSchedulerJobExtensionPoint ext : pluginRgty.getExtensionList(TakeAutoScalingSchedulerJobExtensionPoint.class)) {
            code = ext.allowStateChange(getTargetResourceUuid());
        }
        return code;
    }

    @Override
    public String getType() {
        return SchedulerType.RUN_AUTO_SCALING_GROUP;
    }
}
