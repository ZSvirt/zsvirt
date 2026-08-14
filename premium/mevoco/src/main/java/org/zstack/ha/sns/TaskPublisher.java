package org.zstack.ha.sns;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.progress.TaskTracker;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.message.MessageReply;
import org.zstack.sns.SNSConstants;
import org.zstack.sns.SNSPublishMsg;
import org.zstack.sns.platform.http.SNSSystemHttpEndpointFactory;
import org.zstack.sns.system.SNSSystemAlarmTopicManager;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.Collections;

/**
 * Created by MaJin on 2020/5/26.
 */

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class TaskPublisher {
    @Autowired
    private CloudBus bus;

    public static class Message {
        TaskTracker.Task haProgress;

        Message(TaskTracker.Task task) {
            this.haProgress = task;
        }
    }


    private SNSPublishMsg buildMsg(TaskTracker.Task task) {
        SNSPublishMsg msg = new SNSPublishMsg();
        bus.makeLocalServiceId(msg, SNSConstants.SERVICE_ID);
        msg.setTopicUuid(SNSSystemAlarmTopicManager.SYSTEM_ALARM_TOPIC_UUID);
        msg.setMessage(Collections.singletonMap(SNSSystemHttpEndpointFactory.type.toString(), JSONObjectUtil.toJsonString(new Message(task))));
        msg.setMetadata(Collections.singletonMap(SNSSystemHttpEndpointFactory.type.toString(), Collections.emptyMap()));
        return msg;
    }

    void notify(TaskTracker.Task task) {
        bus.send(buildMsg(task));
    }

    void notify(TaskTracker.Task task, NoErrorCompletion completion) {
        bus.send(buildMsg(task), new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                completion.done();
            }
        });
    }
}
