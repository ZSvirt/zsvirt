package org.zstack.scheduler.iam2;

import org.zstack.header.core.ReturnValueCompletion;

/**
 * @Author: DaoDao
 * @Date: 2021/8/23
 */
public interface CancelIAM2ProjectLoginExpiredExtensionPoint extends IAM2ProjectLoginExpiredExtensionPoint {
    void cancelIAM2ProjectLoginExpired(String projectUuid, String ruleUuid, ReturnValueCompletion completion);
}
