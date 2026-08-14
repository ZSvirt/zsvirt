package org.zstack.billing.generator.pubip.vip;

import org.zstack.billing.generator.BillingVO_;

import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by lining on 2019/3/29.
 */

@StaticMetamodel(PubIpVipBandwidthInBillingVO.class)
public class PubIpVipBandwidthInBillingVO_ extends BillingVO_ {
    public static volatile SingularAttribute<PubIpVipBandwidthInBillingVO, String> vipIp;
    public static volatile SingularAttribute<PubIpVipBandwidthInBillingVO, Long> bandwidthSize;
}
