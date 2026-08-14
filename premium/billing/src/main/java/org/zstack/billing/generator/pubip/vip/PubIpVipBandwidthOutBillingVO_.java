package org.zstack.billing.generator.pubip.vip;

import org.zstack.billing.generator.BillingVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by lining on 2019/3/29.
 */
@StaticMetamodel(PubIpVipBandwidthOutBillingVO.class)
public class PubIpVipBandwidthOutBillingVO_ extends BillingVO_ {
    public static volatile SingularAttribute<PubIpVipBandwidthOutBillingVO, String> vipIp;
    public static volatile SingularAttribute<PubIpVipBandwidthOutBillingVO, Long> bandwidthSize;
}
