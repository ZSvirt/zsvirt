package org.zstack.billing.spendingcalculator.volume.root;

/**
 * Created by lining on 2019/4/3.
 */

import org.zstack.billing.UsageAO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(RootVolumeUsageAO.class)
public class RootVolumeUsageAO_ extends UsageAO_ {
    public static volatile SingularAttribute<RootVolumeUsageAO, Long> id;
    public static volatile SingularAttribute<RootVolumeUsageAO, String> vmUuid;
    public static volatile SingularAttribute<RootVolumeUsageAO, String> volumeUuid;
    public static volatile SingularAttribute<RootVolumeUsageAO, String> volumeStatus;
    public static volatile SingularAttribute<RootVolumeUsageAO, String> volumeName;
    public static volatile SingularAttribute<RootVolumeUsageAO, Long> volumeSize;
    public static volatile SingularAttribute<RootVolumeUsageAO, String> inventory;
    public static volatile SingularAttribute<RootVolumeUsageAO, Timestamp> createDate;
    public static volatile SingularAttribute<RootVolumeUsageAO, Timestamp> lastOpDate;
}
