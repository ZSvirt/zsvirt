package org.zstack.autoscaling;


import java.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.autoscaling.group.instance.APIDeleteAutoScalingGroupInstanceMsg;
import org.zstack.autoscaling.group.instance.APIUpdateAutoScalingGroupInstanceMsg;
import org.zstack.autoscaling.group.instance.AutoScalingGroupInstanceVO;
import org.zstack.autoscaling.group.instance.AutoScalingGroupInstanceVO_;
import org.zstack.autoscaling.group.rule.*;
import org.zstack.autoscaling.group.rule.trigger.APIDeleteAutoScalingRuleTriggerMsg;
import org.zstack.autoscaling.group.rule.trigger.AutoScalingRuleAlarmTriggerVO;
import org.zstack.autoscaling.group.rule.trigger.AutoScalingRuleAlarmTriggerVO_;
import org.zstack.autoscaling.group.rule.trigger.AutoScalingRuleTriggerVO;
import org.zstack.autoscaling.template.AutoScalingVmTemplateSystemTags;
import org.zstack.autoscaling.template.AutoScalingVmTemplateVO;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.tag.TagType;
import org.zstack.network.service.lb.APIDeleteLoadBalancerListenerMsg;
import org.zstack.network.service.lb.APIDeleteLoadBalancerMsg;
import org.zstack.network.service.lb.LoadBalancerVO;
import org.zstack.zwatch.alarm.APIDeleteAlarmMsg;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.autoscaling.template.AutoScalingVmTemplateSystemTags.LOAD_BALANCER_LISTENER_UUIDS_TOKEN;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by lining on 2018/10/9.
 */
@InterceptorForService("autoscaling")
public class AutoScalingInterceptor implements ApiMessageInterceptor, GlobalApiMessageInterceptor {
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIDeleteAutoScalingRuleMsg) {
            validate((APIDeleteAutoScalingRuleMsg) msg);
        } else if (msg instanceof APIDeleteAutoScalingRuleTriggerMsg) {
            validate((APIDeleteAutoScalingRuleTriggerMsg) msg);
        } else if (msg instanceof APIDeleteAutoScalingGroupInstanceMsg) {
            validate((APIDeleteAutoScalingGroupInstanceMsg) msg);
        } else if (msg instanceof APIUpdateAutoScalingRuleMsg) {
            validate((APIUpdateAutoScalingRuleMsg) msg);
        } else if (msg instanceof APIDeleteAlarmMsg) {
            validate((APIDeleteAlarmMsg) msg);
        } else if (msg instanceof APIExecuteAutoScalingRuleMsg) {
            validate((APIExecuteAutoScalingRuleMsg) msg);
        } else if (msg instanceof APIDeleteLoadBalancerMsg) {
            validate((APIDeleteLoadBalancerMsg) msg);
        } else if (msg instanceof APIDeleteLoadBalancerListenerMsg) {
            validate((APIDeleteLoadBalancerListenerMsg) msg);
        } else if (msg instanceof APIUpdateAutoScalingGroupInstanceMsg) {
            validate((APIUpdateAutoScalingGroupInstanceMsg) msg);
        }

        return msg;
    }

    private String findAutoScalingVmTemplateByListener(String listenerUuid) {
        List<SystemTagVO> tags = Q.New(SystemTagVO.class).eq(SystemTagVO_.resourceType, AutoScalingVmTemplateVO.class.getSimpleName()).
                like(SystemTagVO_.tag, AutoScalingVmTemplateSystemTags.LOAD_BALANCER_LISTENER_UUIDS.instantiateTag(
                        map(e(AutoScalingVmTemplateSystemTags.LOAD_BALANCER_LISTENER_UUIDS_TOKEN, String.format("%%%s%%",listenerUuid)))
                )).eq(SystemTagVO_.type, TagType.System).list();
        if (tags.isEmpty()) {
            return null;
        }

        for (SystemTagVO tag: tags) {
            if (!AutoScalingVmTemplateSystemTags.LOAD_BALANCER_LISTENER_UUIDS.isMatch(tag.getTag())) {
                continue;
            }
            String loadBalancerListenerUuidListStr = AutoScalingVmTemplateSystemTags.LOAD_BALANCER_LISTENER_UUIDS.getTokenByTag(tag.getTag(), LOAD_BALANCER_LISTENER_UUIDS_TOKEN);
            if (loadBalancerListenerUuidListStr.contains(listenerUuid)) {
                return tag.getResourceUuid();
            }
        }
        return null;
    }

    private void validate(APIDeleteLoadBalancerMsg msg) {
        LoadBalancerVO lb = dbf.findByUuid(msg.getUuid(), LoadBalancerVO.class);
        if (lb != null && !lb.getListeners().isEmpty()) {
            lb.getListeners().forEach(listener -> {
                String uuid = findAutoScalingVmTemplateByListener(listener.getUuid());
                if (uuid != null) {
                    throw new ApiMessageInterceptionException(operr("listener[uuid:%s] are being used by the autoScalingVmTemplate[%s] and cannot be deleted",
                            listener.getUuid(), uuid));
                }
            });
        }
    }

    private void validate(APIDeleteLoadBalancerListenerMsg msg) {
        String uuid = findAutoScalingVmTemplateByListener(msg.getUuid());
        if (uuid != null) {
            throw new ApiMessageInterceptionException(operr("listener[uuid:%s] are being used by the autoScalingVmTemplate[%s] and cannot be deleted",
                        msg.getUuid(), uuid));
        }
    }

    private void validate(APIUpdateAutoScalingGroupInstanceMsg msg) {
       boolean exists = Q.New(AutoScalingGroupInstanceVO.class)
               .eq(AutoScalingGroupInstanceVO_.instanceUuid, msg.getInstanceUuid())
               .eq(AutoScalingGroupInstanceVO_.scalingGroupUuid, msg.getGroupUuid())
               .isExists();
       if (!exists) {
           throw new ApiMessageInterceptionException(operr("The instance[%s] does not exist in the scaling group[%s]",
                   msg.getInstanceUuid(), msg.getGroupUuid()));
       }
    }

    private void validate(APIUpdateAutoScalingRuleMsg msg) {
        AutoScalingRuleVO ruleVO = dbf.findByUuid(msg.getUuid(), AutoScalingRuleVO.class);
        msg.setAutoScalingGroupUuid(ruleVO.getScalingGroupUuid());
    }

    private void validate(APIDeleteAutoScalingRuleMsg msg) {
        AutoScalingRuleVO ruleVO = dbf.findByUuid(msg.getUuid(), AutoScalingRuleVO.class);
        msg.setAutoScalingGroupUuid(ruleVO.getScalingGroupUuid());
    }

    private void validate(APIDeleteAutoScalingRuleTriggerMsg msg) {
        AutoScalingRuleTriggerVO triggerVO = dbf.findByUuid(msg.getUuid(), AutoScalingRuleTriggerVO.class);
        AutoScalingRuleVO ruleVO = dbf.findByUuid(triggerVO.getRuleUuid(), AutoScalingRuleVO.class);
        msg.setAutoScalingGroupUuid(ruleVO.getScalingGroupUuid());
    }

    private void validate(APIDeleteAutoScalingGroupInstanceMsg msg) {
        AutoScalingGroupInstanceVO instanceVO = dbf.findByUuid(msg.getInstanceUuid(), AutoScalingGroupInstanceVO.class);
        msg.setAutoScalingGroupUuid(instanceVO.getScalingGroupUuid());
    }

    private void validate(APIDeleteAlarmMsg msg) {
        List<AutoScalingRuleAlarmTriggerVO> triggerVOS =  Q.New(AutoScalingRuleAlarmTriggerVO.class)
                .eq(AutoScalingRuleAlarmTriggerVO_.alarmUuid, msg.getAlarmUuid())
                .list();
        if (!triggerVOS.isEmpty()) {
            AutoScalingRuleVO ruleVO = dbf.findByUuid(triggerVOS.get(0).getRuleUuid(), AutoScalingRuleVO.class);
            throw new OperationFailureException(operr("alarm[uuid:%s] are being used by the autoScalingGroup[%s] which cannot be deleted",
                    msg.getAlarmUuid(), ruleVO.getScalingGroupUuid()));
        }
    }

    private void validate(APIExecuteAutoScalingRuleMsg msg) {
        AutoScalingRuleVO ruleVO = dbf.findByUuid(msg.getUuid(), AutoScalingRuleVO.class);
        msg.setAutoScalingGroupUuid(ruleVO.getScalingGroupUuid());

        if (ruleVO.getState() == AutoScalingRuleState.Disabled) {
            throw new OperationFailureException(operr("rule[%s] state is Disabled", msg.getUuid()));
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List<Class> getMessageClassToIntercept() {
        return Arrays.asList(
            APIDeleteLoadBalancerMsg.class,
            APIDeleteLoadBalancerListenerMsg.class,
            APIDeleteAlarmMsg.class
        );
    }

    @Override
    public GlobalApiMessageInterceptor.InterceptorPosition getPosition() {
        return GlobalApiMessageInterceptor.InterceptorPosition.END;
    }

}
