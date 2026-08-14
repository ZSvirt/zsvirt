package org.zstack.cloudformation;

/**
 * Created by mingjian.deng on 2018/6/5.
 */
public interface CloudFormationConstant {
    String SERVICE_ID = "cloudformation";
    String ACTION_CATEGORY = "cloudformation";

    int maxLength = 4194304;
    int paramMaxLength = 1048576;

    String version = ResourceStackVersion.v1.toString();

    String systemTemplateFolder = "cloudFormationTemplates";
}
