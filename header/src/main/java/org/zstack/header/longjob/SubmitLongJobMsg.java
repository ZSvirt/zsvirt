package org.zstack.header.longjob;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by GuoYi on 3/27/18
 */
public class SubmitLongJobMsg extends NeedReplyMessage {
    private String name;
    private String description;
    private String jobName;
    private String jobData;
    private String targetResourceUuid;
    private String jobUuid;
    private String resourceUuid;
    private String accountUuid;
    private String jobRequestUuid;
    private String parentUuid;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobData() {
        return jobData;
    }

    public void setJobData(String jobData) {
        this.jobData = jobData;
    }

    public String getTargetResourceUuid() {
        return targetResourceUuid;
    }

    public void setTargetResourceUuid(String targetResourceUuid) {
        this.targetResourceUuid = targetResourceUuid;
    }

    public String getJobUuid() {
        return jobUuid;
    }

    public void setJobUuid(String jobUuid) {
        this.jobUuid = jobUuid;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public static SubmitLongJobMsg valueOf(final APISubmitLongJobMsg msg) {
        SubmitLongJobMsg smsg = new SubmitLongJobMsg();
        smsg.setDescription(msg.getDescription());
        smsg.setJobData(msg.getJobData());
        smsg.setJobName(msg.getJobName());
        smsg.setName(msg.getName());
        smsg.setTargetResourceUuid(msg.getTargetResourceUuid());
        smsg.setResourceUuid(msg.getResourceUuid());
        smsg.setSystemTags(msg.getSystemTags());
        smsg.setUserTags(msg.getUserTags());
        smsg.setAccountUuid(msg.getSession().getAccountUuid());
        return smsg;
    }

    public String getJobRequestUuid() {
        return jobRequestUuid;
    }

    public void setJobRequestUuid(String jobRequestUuid) {
        this.jobRequestUuid = jobRequestUuid;
    }

    public String getParentUuid() {
        return parentUuid;
    }

    /**
     * @see LongJobVO#getUuid()
     */
    public void setParentUuid(String parentUuid) {
        this.parentUuid = parentUuid;
    }
}
