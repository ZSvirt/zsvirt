package org.zstack.core.workflow;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.util.Date;

@StaticMetamodel(WorkFlowChainVO.class)
public class WorkFlowChainVO_ {
    public static volatile SingularAttribute<WorkFlowChainVO, String> uuid;
    public static volatile SingularAttribute<WorkFlowChainVO, String> name;
    public static volatile SingularAttribute<WorkFlowChainVO, String> owner;
    public static volatile SingularAttribute<WorkFlowChainVO, String> reason;
    public static volatile SingularAttribute<WorkFlowChainVO, WorkFlowChainState> state;
    public static volatile SingularAttribute<WorkFlowChainVO, Integer> totalWorkFlows;
    public static volatile SingularAttribute<WorkFlowChainVO, Integer> currentPosition;
    public static volatile SingularAttribute<WorkFlowChainVO, Date> operationDate;
}
