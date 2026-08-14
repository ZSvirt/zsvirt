package org.zstack.billing.spendingcalculator.volume.root;

import org.zstack.billing.spendingcalculator.volume.data.DataVolumeUsageExtensionVO;
import org.zstack.billing.spendingcalculator.volume.root.RootVolumeUsageExtensionVO;
import org.zstack.billing.spendingcalculator.volume.root.RootVolumeUsageVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by lining on 2019/5/10.
 */
@StaticMetamodel(RootVolumeUsageExtensionVO.class)
public class RootVolumeUsageExtensionVO_ extends RootVolumeUsageVO_ {
    public static volatile SingularAttribute<DataVolumeUsageExtensionVO, String> resourcePriceUserConfig;
}
