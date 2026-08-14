package org.zstack.header.agent.versioncontrol;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(AgentVersionVO.class)
public class AgentVersionVO_ {
    public static volatile SingularAttribute<AgentVersionVO, String> uuid;
    public static volatile SingularAttribute<AgentVersionVO, String> agentType;
    public static volatile SingularAttribute<AgentVersionVO, String> currentVersion;
    public static volatile SingularAttribute<AgentVersionVO, String> expectVersion;
    public static volatile SingularAttribute<AgentVersionVO, Timestamp> lastOpDate;
    public static volatile SingularAttribute<AgentVersionVO, Timestamp> createDate;
}
