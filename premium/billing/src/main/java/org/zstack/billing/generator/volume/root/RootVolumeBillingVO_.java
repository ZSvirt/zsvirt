package org.zstack.billing.generator.volume.root;

/**
 * Created by lining on 2019/4/2.
 */

import org.zstack.billing.generator.BillingVO_;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(RootVolumeBillingVO.class)
public class RootVolumeBillingVO_ extends BillingVO_ {
    public static volatile SingularAttribute<RootVolumeBillingVO, String> vmInstanceUuid;
    public static volatile SingularAttribute<RootVolumeBillingVO, Long> volumeSize;
}
