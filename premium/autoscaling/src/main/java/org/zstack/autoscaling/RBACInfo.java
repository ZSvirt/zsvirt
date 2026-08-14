package org.zstack.autoscaling;

import org.zstack.autoscaling.template.AutoScalingTemplateVO;
import org.zstack.header.description.PackageDescription;

import org.zstack.autoscaling.group.APIChangeAutoScalingGroupStateMsg;
import org.zstack.autoscaling.group.APICreateAutoScalingGroupMsg;
import org.zstack.autoscaling.group.APIDeleteAutoScalingGroupMsg;
import org.zstack.autoscaling.group.APIQueryAutoScalingGroupMsg;
import org.zstack.autoscaling.group.APIUpdateAutoScalingGroupMsg;
import org.zstack.autoscaling.group.instance.APIQueryAutoScalingGroupInstanceMsg;
import org.zstack.autoscaling.group.rule.APICreateAutoScalingGroupAddingNewInstanceRuleMsg;
import org.zstack.autoscaling.group.rule.APICreateAutoScalingGroupRemovalInstanceRuleMsg;
import org.zstack.autoscaling.group.rule.APIDeleteAutoScalingRuleMsg;
import org.zstack.autoscaling.group.rule.APIExecuteAutoScalingRuleMsg;
import org.zstack.autoscaling.group.rule.APIQueryAutoScalingRuleMsg;
import org.zstack.autoscaling.group.rule.APIUpdateAutoScalingGroupAddingNewInstanceRuleMsg;
import org.zstack.autoscaling.group.rule.APIUpdateAutoScalingGroupRemovalInstanceRuleMsg;
import org.zstack.autoscaling.group.rule.trigger.APIQueryAutoScalingRuleTriggerMsg;
import org.zstack.autoscaling.template.APIQueryAutoScalingVmTemplateMsg;
import org.zstack.header.search.SearchConstant;
/**
 * Created by lining on 2018/10/18.
 */
public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "autoscaling";
    }

    {
        permissionBuilder()
                .targetResources(AutoScalingTemplateVO.class)
                .adminOnlyForAll()
                .communityAvailable()
                .build();

        apis()
                .api(
                        APIChangeAutoScalingGroupStateMsg.class,
                        APICreateAutoScalingGroupMsg.class,
                        APIDeleteAutoScalingGroupMsg.class,
                        APIUpdateAutoScalingGroupMsg.class
                )
                .toService(AutoScalingConstants.SERVICE_ID)
                .build();
        apis()
                .api(APIQueryAutoScalingGroupMsg.class)
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
        apis()
                .inPackage("org.zstack.autoscaling.group.activity")
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
        apis()
                .inPackage("org.zstack.autoscaling.group.instance")
                .toService(AutoScalingConstants.SERVICE_ID)
                .build();
        apis()
                .api(APIQueryAutoScalingGroupInstanceMsg.class)
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
        apis()
                .api(
                        APICreateAutoScalingGroupAddingNewInstanceRuleMsg.class,
                        APICreateAutoScalingGroupRemovalInstanceRuleMsg.class,
                        APIDeleteAutoScalingRuleMsg.class,
                        APIExecuteAutoScalingRuleMsg.class,
                        APIUpdateAutoScalingGroupAddingNewInstanceRuleMsg.class,
                        APIUpdateAutoScalingGroupRemovalInstanceRuleMsg.class
                )
                .toService(AutoScalingConstants.SERVICE_ID)
                .build();
        apis()
                .api(APIQueryAutoScalingRuleMsg.class)
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
        apis()
                .inPackage("org.zstack.autoscaling.group.rule.trigger")
                .toService(AutoScalingConstants.SERVICE_ID)
                .build();
        apis()
                .api(APIQueryAutoScalingRuleTriggerMsg.class)
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
        apis()
                .inPackage("org.zstack.autoscaling.template")
                .toService(AutoScalingConstants.SERVICE_ID)
                .build();
        apis()
                .api(APIQueryAutoScalingVmTemplateMsg.class)
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
    }
}
