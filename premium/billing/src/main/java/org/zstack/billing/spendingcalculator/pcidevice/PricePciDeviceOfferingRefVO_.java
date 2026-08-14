package org.zstack.billing.spendingcalculator.pcidevice;

/**
 * Created by shixin.ruan on 2018/05/04.
 */

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(PricePciDeviceOfferingRefVO.class)
public class PricePciDeviceOfferingRefVO_ {
    public static volatile SingularAttribute<PricePciDeviceOfferingRefVO, Long> id;
    public static volatile SingularAttribute<PricePciDeviceOfferingRefVO, String> priceUuid;
    public static volatile SingularAttribute<PricePciDeviceOfferingRefVO, String> pciDeviceOfferingUuid;
    public static volatile SingularAttribute<PricePciDeviceOfferingRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<PricePciDeviceOfferingRefVO, Timestamp> lastOpDate;
}
