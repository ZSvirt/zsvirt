package org.zstack.header.cloudformation;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by mingjian.deng on 2018/6/12.
 */
@StaticMetamodel(CloudFormationStackResourceRefVO.class)
public class CloudFormationStackResourceRefVO_ {
    public static volatile SingularAttribute<CloudFormationStackResourceRefVO, Long> id;
    public static volatile SingularAttribute<CloudFormationStackResourceRefVO, String> stackUuid;
    public static volatile SingularAttribute<CloudFormationStackResourceRefVO, String> resourceUuid;
    public static volatile SingularAttribute<CloudFormationStackResourceRefVO, String> resourceName;
    public static volatile SingularAttribute<CloudFormationStackResourceRefVO, String> resourceType;
    public static volatile SingularAttribute<CloudFormationStackResourceRefVO, Boolean> reserve;
    public static volatile SingularAttribute<CloudFormationStackResourceRefVO, Integer> round;
}
