package org.zstack.log;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Level;

public class LogConfigurationStruct {
    private String uuid;
    private String name;
    private String description;
    private String accountUuid;
    private String category;
    private String type;
    private String level;
    private String configuration;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

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

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Level getLog4jOfLevel() {
        if (StringUtils.isBlank(level)) {
            return Level.ALL;
        }
        return Level.getLevel(level);
    }
}
