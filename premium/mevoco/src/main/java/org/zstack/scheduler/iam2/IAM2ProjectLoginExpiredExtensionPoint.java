package org.zstack.scheduler.iam2;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.Message;

/**
 * @Author: DaoDao
 * @Date: 2021/8/23
 */
public interface IAM2ProjectLoginExpiredExtensionPoint {
    Message buildRequest(String projectUuid, String state);
    ErrorCode allowStateChange(String projectUuid);
}
