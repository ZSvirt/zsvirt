package org.zstack.scheduler;

import org.zstack.header.message.NeedReplyMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by MaJin on 2019/4/17.
 */
public class RunSchedulerTriggerMsg extends NeedReplyMessage {
    private String triggerUuid;
    private List<String> jobUuids;
    private Map<String, List<String>> groupJobUuids = new HashMap<>();
    private String fireInstanceId;

    public String getTriggerUuid() {
        return triggerUuid;
    }

    public void setTriggerUuid(String triggerUuid) {
        this.triggerUuid = triggerUuid;
    }

    public List<String> getJobUuids() {
        return jobUuids;
    }

    public void setJobUuids(List<String> jobUuids) {
        this.jobUuids = jobUuids;
    }

    public Map<String, List<String>> getGroupJobUuids() {
        return groupJobUuids;
    }

    public void setGroupJobUuids(Map<String, List<String>> groupJobUuids) {
        this.groupJobUuids = groupJobUuids;
    }

    public String getFireInstanceId() {
        return fireInstanceId;
    }

    public void setFireInstanceId(String fireInstanceId) {
        this.fireInstanceId = fireInstanceId;
    }
}
