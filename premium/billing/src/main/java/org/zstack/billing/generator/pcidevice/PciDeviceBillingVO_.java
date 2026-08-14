package org.zstack.billing.generator.pcidevice;
import org.zstack.billing.generator.BillingVO_;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by jie.wang on 2019/4/4.
 */

@StaticMetamodel(PciDeviceBillingVO.class)
public class PciDeviceBillingVO_ extends BillingVO_ {
    public static volatile SingularAttribute<PciDeviceBillingVO, String> vmName;
}
