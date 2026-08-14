package org.zstack.billing.spendingcalculator.vip;

import org.zstack.billing.UsageAO_;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by lining on 2019/4/3.
 */
@StaticMetamodel(PubIpVipBandwidthUsageAO.class)
public class PubIpVipBandwidthUsageAO_ extends UsageAO_ {
    public static volatile SingularAttribute<PubIpVipBandwidthUsageAO, Long> id;
    public static volatile SingularAttribute<PubIpVipBandwidthUsageAO, String> vipUuid;
    public static volatile SingularAttribute<PubIpVipBandwidthUsageAO, String> vipIp;
    public static volatile SingularAttribute<PubIpVipBandwidthUsageAO, String> vipStatus;
    public static volatile SingularAttribute<PubIpVipBandwidthUsageAO, String> vipName;
    public static volatile SingularAttribute<PubIpVipBandwidthUsageAO, String> l3NetworkUuid;
    public static volatile SingularAttribute<PubIpVipBandwidthUsageAO, Long> bandwidthOut;
    public static volatile SingularAttribute<PubIpVipBandwidthUsageAO, Long> bandwidthIn;
    public static volatile SingularAttribute<PubIpVipBandwidthUsageAO, String> inventory;
    public static volatile SingularAttribute<PubIpVipBandwidthUsageAO, Timestamp> createDate;
    public static volatile SingularAttribute<PubIpVipBandwidthUsageAO, Timestamp> lastOpDate;
}
