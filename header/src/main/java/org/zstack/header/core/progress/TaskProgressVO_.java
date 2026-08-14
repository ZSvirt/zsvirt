package org.zstack.header.core.progress;


import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(TaskProgressVO.class)
public class TaskProgressVO_ {
    public static volatile SingularAttribute<TaskProgressVO, Long> id;
    public static volatile SingularAttribute<TaskProgressVO, String> apiId;
    public static volatile SingularAttribute<TaskProgressVO, String> content;
    public static volatile SingularAttribute<TaskProgressVO, String> opaque;
    public static volatile SingularAttribute<TaskProgressVO, Long> createTime;
    public static volatile SingularAttribute<TaskProgressVO, Long> lastOpTime;
    public static volatile SingularAttribute<TaskProgressVO, Long> currentStep;
    public static volatile SingularAttribute<TaskProgressVO, Long> totalStep;
}
