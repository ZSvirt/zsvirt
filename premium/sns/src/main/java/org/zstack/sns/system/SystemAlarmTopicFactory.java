package org.zstack.sns.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.sns.SNSTopic;
import org.zstack.sns.SNSTopicFactory;
import org.zstack.sns.SNSTopicVO;

public class SystemAlarmTopicFactory implements SNSTopicFactory {
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public SNSTopic createSNSTopic(String uuid) {
        if (!SNSSystemAlarmTopicManager.SYSTEM_ALARM_TOPIC_UUID.equals(uuid)) {
            return null;
        }

        return new SystemAlarmTopic(dbf.findByUuid(uuid, SNSTopicVO.class));
    }
}
