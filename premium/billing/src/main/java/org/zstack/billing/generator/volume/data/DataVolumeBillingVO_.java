package org.zstack.billing.generator.volume.data;

/**
 * Created by lining on 2019/4/2.
 */

import org.zstack.billing.generator.BillingVO_;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(DataVolumeBillingVO.class)
public class DataVolumeBillingVO_ extends BillingVO_ {
    public static volatile SingularAttribute<DataVolumeBillingVO, String> vmInstanceUuid;
    public static volatile SingularAttribute<DataVolumeBillingVO, Long> volumeSize;
}
