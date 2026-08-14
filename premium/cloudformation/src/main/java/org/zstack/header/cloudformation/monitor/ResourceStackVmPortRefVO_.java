package org.zstack.header.cloudformation.monitor;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by mingjian.deng on 2019/11/22.
 */
@StaticMetamodel(ResourceStackVmPortRefVO.class)
public class ResourceStackVmPortRefVO_ {
    public static volatile SingularAttribute<ResourceStackVmPortRefVO, Long> id;
    public static volatile SingularAttribute<ResourceStackVmPortRefVO, String> stackUuid;
    public static volatile SingularAttribute<ResourceStackVmPortRefVO, String> vmInstanceUuid;
    public static volatile SingularAttribute<ResourceStackVmPortRefVO, Integer> port;
    public static volatile SingularAttribute<ResourceStackVmPortRefVO, String> status;
    public static volatile SingularAttribute<ResourceStackVmPortRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<ResourceStackVmPortRefVO, Timestamp> lastOpDate;
}
