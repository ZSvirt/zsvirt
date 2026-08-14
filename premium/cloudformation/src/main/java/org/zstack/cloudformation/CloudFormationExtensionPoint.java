package org.zstack.cloudformation;

import org.zstack.cloudformation.template.struct.StackData;

/**
 * Created by mingjian.deng on 2018/6/27.
 */
public interface CloudFormationExtensionPoint {
    void beforeCloudFormationAction(Object action);
    void afterCloudFormationAction(Object action, Object result, StackData stackData);
}
