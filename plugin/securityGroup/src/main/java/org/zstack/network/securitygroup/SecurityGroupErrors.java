package org.zstack.network.securitygroup;

/**
 */
public enum SecurityGroupErrors {
    ADD_NIC_ERROR(1000),
    RESOURCE_NOT_EXIST_ERROR(1001),
    RULE_DUPLICATE_ERROR(1002),
    RULE_FILED_CONFLICT_ERROR(1003),
    RULE_FILED_NOT_SUPPORT_ERROR(1004),
    RULE_PORT_FIELD_ERROR(1005),
    RULE_IP_FIELD_ERROR(1006),
    RULE_CHECK_OK(2000);

    private String code;

    private SecurityGroupErrors(int id) {
        code = String.format("SG.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
