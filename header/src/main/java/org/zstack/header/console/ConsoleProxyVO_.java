package org.zstack.header.console;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 7:23 PM
 * To change this template use File | Settings | File Templates.
 */
@StaticMetamodel(ConsoleProxyVO.class)
public class ConsoleProxyVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<ConsoleProxyVO, String> vmInstanceUuid;
    public static volatile SingularAttribute<ConsoleProxyVO, String> proxyHostname;
    public static volatile SingularAttribute<ConsoleProxyVO, Integer> proxyPort;
    public static volatile SingularAttribute<ConsoleProxyVO, String> targetHostname;
    public static volatile SingularAttribute<ConsoleProxyVO, Integer> targetPort;
    public static volatile SingularAttribute<ConsoleProxyVO, String> scheme;
    public static volatile SingularAttribute<ConsoleProxyVO, String> agentIp;
    public static volatile SingularAttribute<ConsoleProxyVO, String> agentType;
    public static volatile SingularAttribute<ConsoleProxyVO, String> proxyIdentity;
    public static volatile SingularAttribute<ConsoleProxyVO, String> token;
    public static volatile SingularAttribute<ConsoleProxyVO, ConsoleProxyStatus> status;
    public static volatile SingularAttribute<ConsoleProxyVO, Timestamp> createDate;
    public static volatile SingularAttribute<ConsoleProxyVO, Timestamp> lastOpDate;
    public static volatile SingularAttribute<ConsoleProxyVO, Timestamp> expiredDate;
}
