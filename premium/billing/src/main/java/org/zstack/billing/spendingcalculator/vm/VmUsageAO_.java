package org.zstack.billing.spendingcalculator.vm;

import org.zstack.billing.UsageAO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by lining on 2019/3/29.
 */
@StaticMetamodel(VmUsageAO.class)
public class VmUsageAO_ extends UsageAO_ {
    public static volatile SingularAttribute<VmUsageAO, Long> id;
    public static volatile SingularAttribute<VmUsageAO, String> vmUuid;
    public static volatile SingularAttribute<VmUsageAO, String> state;
    public static volatile SingularAttribute<VmUsageAO, String> name;
    public static volatile SingularAttribute<VmUsageAO, Integer> cpuNum;
    public static volatile SingularAttribute<VmUsageAO, Long> memorySize;
    public static volatile SingularAttribute<VmUsageAO, Long> rootVolumeSize;
    public static volatile SingularAttribute<VmUsageAO, String> inventory;
    public static volatile SingularAttribute<VmUsageAO, Timestamp> createDate;
    public static volatile SingularAttribute<VmUsageAO, Timestamp> lastOpDate;
}
