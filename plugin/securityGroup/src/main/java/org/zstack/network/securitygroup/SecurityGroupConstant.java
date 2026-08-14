package org.zstack.network.securitygroup;

public interface SecurityGroupConstant {
    public static String SERVICE_ID = "securityGroup";

    public static String ACTION_CATEGORY = "securityGroup";

    public static String SECURITY_GROUP_PROVIDER_TYPE = "SecurityGroup";
    public static String SECURITY_GROUP_NETWORK_SERVICE_TYPE = "SecurityGroup";
    public static String WORLD_OPEN_CIDR = "0.0.0.0/0";
    public static String WORLD_OPEN_CIDR_IPV6 = "::/0";
    public static String IP_SPLIT = ",";
    public static String CIDR_SPLIT = "/";
    public static String RANGE_SPLIT = "-";
    public static String DEFAULT_RULE_DESCRIPTION = "default rule";
    
    public static int DEFAULT_RULE_PRIORITY = 0;
    public static int LOWEST_RULE_PRIORITY = -1;
    public static int IP_GROUP_NUMBER_LIMIT = 10;
    public static int PORT_GROUP_NUMBER_LIMIT = 10;
    public static int PORT_NUMBER_MAX = 65535;
    public static int PORT_NUMBER_MIN = 0;
    int ONE_API_RULES_MAX_NUM = 2000;

    public static enum Param {
        SECURITY_GROUP_RULE_VO,
        SECURITY_GROUP_VO,
        SECURITY_GROUP_UUIDS,
        VM_NIC_UUIDS,
        HOST_SECURITY_GROUP_MEMBERS_TO
    }
}
