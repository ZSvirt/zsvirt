package org.zstack.sns;

/**
 * Created by GuoYi on 2021/1/19.
 */
public interface SNSGlobalPropertyExtension {
    void afterSNSGlobalPropertyUpdated(SNSCommands.UpdateSNSGlobalPropertyCmd cmd);
}
