package org.zstack.header.console;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by xing5 on 2016/3/15.
 */
@StaticMetamodel(ConsoleProxyAgentVO.class)
public class ConsoleProxyAgentVO_ {
    public static volatile SingularAttribute<ConsoleProxyAgentVO, String> uuid;
    public static volatile SingularAttribute<ConsoleProxyAgentVO, String> description;
    public static volatile SingularAttribute<ConsoleProxyAgentVO, String> managementIp;
    public static volatile SingularAttribute<ConsoleProxyAgentVO, String> consoleProxyOverriddenIp;
    public static volatile SingularAttribute<ConsoleProxyAgentVO, Integer> consoleProxyPort;
    public static volatile SingularAttribute<ConsoleProxyAgentVO, String> type;
    public static volatile SingularAttribute<ConsoleProxyAgentVO, String> status;
    public static volatile SingularAttribute<ConsoleProxyAgentVO, String> state;
    public static volatile SingularAttribute<ConsoleProxyAgentVO, Timestamp> createDate;
    public static volatile SingularAttribute<ConsoleProxyAgentVO, Timestamp> lastOpDate;
}
