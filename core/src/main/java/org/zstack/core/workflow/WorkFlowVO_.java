package org.zstack.core.workflow;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.util.Date;

@StaticMetamodel(WorkFlowVO.class)
public class WorkFlowVO_ {
    public static volatile SingularAttribute<WorkFlowVO, Long> id;
    public static volatile SingularAttribute<WorkFlowVO, String> chainUuid;
    public static volatile SingularAttribute<WorkFlowVO, String> name;
    public static volatile SingularAttribute<WorkFlowVO, WorkFlowState> state;
    public static volatile SingularAttribute<WorkFlowVO, String> reason;
    public static volatile SingularAttribute<WorkFlowVO, Integer> position;
    public static volatile SingularAttribute<WorkFlowVO, Date> operationDate;
}
