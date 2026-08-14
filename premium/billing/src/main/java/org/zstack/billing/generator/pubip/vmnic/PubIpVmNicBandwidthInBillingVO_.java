package org.zstack.billing.generator.pubip.vmnic;

import org.zstack.billing.generator.BillingVO_;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by lining on 2019/4/6.
 */

@StaticMetamodel(PubIpVmNicBandwidthInBillingVO.class)
public class PubIpVmNicBandwidthInBillingVO_ extends BillingVO_ {
    public static volatile SingularAttribute<PubIpVmNicBandwidthInBillingVO, String> vmNicIp;
    public static volatile SingularAttribute<PubIpVmNicBandwidthInBillingVO, Long> bandwidthSize;
}
