package org.zstack.billing.spendingcalculator.vmnic;

import org.zstack.billing.UsageAO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by lining on 2019/4/3.
 */
@StaticMetamodel(PubIpVmNicBandwidthUsageAO.class)
public class PubIpVmNicBandwidthUsageAO_ extends UsageAO_ {
    public static volatile SingularAttribute<PubIpVmNicBandwidthUsageAO, Long> id;
    public static volatile SingularAttribute<PubIpVmNicBandwidthUsageAO, String> vmNicUuid;
    public static volatile SingularAttribute<PubIpVmNicBandwidthUsageAO, String> vmNicIp;
    public static volatile SingularAttribute<PubIpVmNicBandwidthUsageAO, String> vmNicStatus;
    public static volatile SingularAttribute<PubIpVmNicBandwidthUsageAO, String> l3NetworkUuid;
    public static volatile SingularAttribute<PubIpVmNicBandwidthUsageAO, Long> bandwidthOut;
    public static volatile SingularAttribute<PubIpVmNicBandwidthUsageAO, Long> bandwidthIn;
    public static volatile SingularAttribute<PubIpVmNicBandwidthUsageAO, String> inventory;
    public static volatile SingularAttribute<PubIpVmNicBandwidthUsageAO, Timestamp> createDate;
    public static volatile SingularAttribute<PubIpVmNicBandwidthUsageAO, Timestamp> lastOpDate;
    public static volatile SingularAttribute<PubIpVmNicBandwidthUsageAO, String> vmInstanceUuid;
}
