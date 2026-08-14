package org.zstack.storage.backup;

import org.zstack.header.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.progress.TaskTracker;
import org.zstack.sns.SNSConstants;
import org.zstack.sns.SNSPublishMsg;
import org.zstack.sns.platform.http.SNSSystemHttpEndpointFactory;
import org.zstack.sns.system.SNSSystemAlarmTopicManager;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by MaJin on 2019/3/13.
 */

public class SchedulerBackupNotification implements Component {
    @Autowired
    private CloudBus bus;

    private List<String> registerTaskName = Arrays.asList(VolumeBackupTracker.TASK_NAME, VmBackupTracker.TASK_NAME);

    @Override
    public boolean start() {
        registerTaskName.forEach(name -> TaskTracker.registerConsumer(name, task -> publish(new Message(task))));
        return true;
    }

    @Override
    public boolean stop() {
        return false;
    }

    public static class Message {
        TaskTracker.Task schedulerProcess;

        Message(TaskTracker.Task task) {
            this.schedulerProcess = task;
        }
    }

    public void publish(Message message) {
        SNSPublishMsg msg = new SNSPublishMsg();
        bus.makeLocalServiceId(msg, SNSConstants.SERVICE_ID);
        msg.setTopicUuid(SNSSystemAlarmTopicManager.SYSTEM_ALARM_TOPIC_UUID);
        msg.setMessage(Collections.singletonMap(SNSSystemHttpEndpointFactory.type.toString(), JSONObjectUtil.toJsonString(message)));
        msg.setMetadata(Collections.singletonMap(SNSSystemHttpEndpointFactory.type.toString(), Collections.emptyMap()));
        bus.send(msg);
    }
}

