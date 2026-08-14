package org.zstack.scheduler.iam2;

import org.zstack.header.core.ReturnValueCompletion;

/**
 * @Author: DaoDao
 * @Date: 2021/8/23
 */
public interface AddIAM2ProjectLoginExpiredExtensionPoint extends IAM2ProjectLoginExpiredExtensionPoint {
    void takeIAM2ProjectLoginExpired(String projectUuid, String ruleUuid, ReturnValueCompletion completion);
}
