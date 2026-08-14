package org.zstack.autoscaling.template;

import org.zstack.header.vo.*;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;
import java.util.Set;

/**
 * Create by weiwang at 2018/8/16
 */
@StaticMetamodel(AutoScalingVmTemplateVO.class)
public class AutoScalingVmTemplateVO_ extends AutoScalingTemplateVO_ {
    public static volatile SingularAttribute<AutoScalingVmTemplateVO, String> vmInstanceName;
    public static volatile SingularAttribute<AutoScalingVmTemplateVO, String> vmInstanceType;
    public static volatile SingularAttribute<AutoScalingVmTemplateVO, String> vmInstanceDescription;
    public static volatile SingularAttribute<AutoScalingVmTemplateVO, String> vmInstanceOfferingUuid;
    public static volatile SingularAttribute<AutoScalingVmTemplateVO, String> l3NetworkUuids;
    public static volatile SingularAttribute<AutoScalingVmTemplateVO, String> rootDiskOfferingUuid;
    public static volatile SingularAttribute<AutoScalingVmTemplateVO, String> dataDiskOfferingUuids;
    public static volatile SingularAttribute<AutoScalingVmTemplateVO, String> vmInstanceZoneUuid;
    public static volatile SingularAttribute<AutoScalingVmTemplateVO, String> vmInstanceClusterUuid;
    public static volatile SingularAttribute<AutoScalingVmTemplateVO, String> hostUuid;
    public static volatile SingularAttribute<AutoScalingVmTemplateVO, String> primaryStorageUuidForRootVolume;
    public static volatile SingularAttribute<AutoScalingVmTemplateVO, String> defaultL3NetworkUuid;
    public static volatile SingularAttribute<AutoScalingVmTemplateVO, String> strategy;
}
