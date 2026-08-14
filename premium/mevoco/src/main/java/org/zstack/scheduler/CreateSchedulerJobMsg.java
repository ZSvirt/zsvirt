package org.zstack.scheduler;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.scheduler.CreateSchedulerJobDescMsg;

import java.util.Map;

/**
 * @Author: DaoDao
 * @Date: 2021/8/23
 */
public class CreateSchedulerJobMsg extends NeedReplyMessage implements CreateSchedulerJobDescMsg {
    private String name;
    private String description;
    private String targetResourceUuid;
    private String type;
    private Map<String, String> parameters;
    private String accountUuid;
    private String uuid;

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

    public String getTargetResourceUuid() {
        return targetResourceUuid;
    }

    public void setTargetResourceUuid(String targetResourceUuid) {
        this.targetResourceUuid = targetResourceUuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public String getResourceUuid() {
        return uuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}

