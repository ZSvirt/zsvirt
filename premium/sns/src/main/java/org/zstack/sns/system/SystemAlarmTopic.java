package org.zstack.sns.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.sns.SNSPublishMsg;
import org.zstack.sns.SNSTopicBase;
import org.zstack.sns.SNSTopicVO;
import org.zstack.sns.platform.system.SNSSystemPlatformFactory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class SystemAlarmTopic extends SNSTopicBase {
    private static final CLogger logger = Utils.getLogger(SystemAlarmTopic.class);

    @Autowired
    private SNSSystemPlatformFactory systemPlatformFactory;

    public SystemAlarmTopic(SNSTopicVO self) {
        super(self);
    }

    @Override
    protected void handle(SNSPublishMsg msg){
        new While<>(systemPlatformFactory.getSNSApplicationSystemEndpoints()).all((e, completion) ->
                e.publish(createMessageStruct(msg, e.getEndpointType()), new Completion(completion) {
            @Override
            public void success() {
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("failed to publish to internal endpoint[%s], %s", e.getClass(), errorCode));
                completion.done();
            }
        })).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                SystemAlarmTopic.super.handle(msg);
            }
        });
    }
}
