package org.zstack.billing.spendingcalculator.volume.data;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by lining on 2019/5/10.
 */
@StaticMetamodel(DataVolumeUsageExtensionVO.class)
public class DataVolumeUsageExtensionVO_ extends DataVolumeUsageVO_ {
    public static volatile SingularAttribute<DataVolumeUsageExtensionVO, String> resourcePriceUserConfig;
}
