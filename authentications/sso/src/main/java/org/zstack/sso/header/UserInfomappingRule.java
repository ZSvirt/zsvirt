package org.zstack.sso.header;

/**
 * @Author: DaoDao
 * @Date: 2022/8/25
 */
public class UserInfomappingRule {
    private String name;
    private String attribute;
    private RuleAttributeType type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public RuleAttributeType getType() {
        return type;
    }

    public void setType(RuleAttributeType type) {
        this.type = type;
    }

    public enum RuleAttributeType {
        SYSTEM,
        CUSTOM
    }
}
