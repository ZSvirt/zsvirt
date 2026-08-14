package org.zstack.billing.spendingcalculator.volume.data;

/**
 * Created by lining on 2019/4/3.
 */

import org.zstack.billing.UsageAO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(DataVolumeUsageAO.class)
public class DataVolumeUsageAO_ extends UsageAO_ {
    public static volatile SingularAttribute<DataVolumeUsageAO, Long> id;
    public static volatile SingularAttribute<DataVolumeUsageAO, String> volumeUuid;
    public static volatile SingularAttribute<DataVolumeUsageAO, String> volumeStatus;
    public static volatile SingularAttribute<DataVolumeUsageAO, String> volumeName;
    public static volatile SingularAttribute<DataVolumeUsageAO, Long> volumeSize;
    public static volatile SingularAttribute<DataVolumeUsageAO, String> inventory;
    public static volatile SingularAttribute<DataVolumeUsageAO, Timestamp> createDate;
    public static volatile SingularAttribute<DataVolumeUsageAO, Timestamp> lastOpDate;
}
