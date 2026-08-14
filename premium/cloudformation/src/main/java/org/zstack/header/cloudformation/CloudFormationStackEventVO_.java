package org.zstack.header.cloudformation;

import org.zstack.cloudformation.StackEventStatus;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by mingjian.deng on 2018/6/14.
 */
@StaticMetamodel(CloudFormationStackEventVO.class)
public class CloudFormationStackEventVO_ {
    public static volatile SingularAttribute<CloudFormationStackEventVO, Long> id;
    public static volatile SingularAttribute<CloudFormationStackEventVO, String> action;
    public static volatile SingularAttribute<CloudFormationStackEventVO, String> resourceName;
    public static volatile SingularAttribute<CloudFormationStackEventVO, String> description;
    public static volatile SingularAttribute<CloudFormationStackEventVO, String> content;
    public static volatile SingularAttribute<CloudFormationStackEventVO, StackEventStatus> actionStatus;
    public static volatile SingularAttribute<CloudFormationStackEventVO, String> stackUuid;
    public static volatile SingularAttribute<CloudFormationStackEventVO, String> duration;
    public static volatile SingularAttribute<CloudFormationStackEventVO, Timestamp> createDate;
    public static volatile SingularAttribute<CloudFormationStackEventVO, Timestamp> lastOpDate;
}