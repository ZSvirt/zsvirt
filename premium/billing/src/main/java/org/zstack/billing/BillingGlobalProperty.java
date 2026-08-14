package org.zstack.billing;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * Created by xing5 on 2016/6/24.
 */
@GlobalPropertyDefinition
public class BillingGlobalProperty {
    @GlobalProperty(name="tapResourcesForBilling", defaultValue = "false")
    public static boolean TAP_RESOURCE_FOR_BILLING;

    @GlobalProperty(name="generateBillsImmediately", defaultValue = "false")
    public static boolean GENERATE_BILLS_IMMEDIATELY;

    @GlobalProperty(name="generatePriceEndDate", defaultValue = "false")
    public static boolean GENERATE_PRICE_END_DATE_IN_LONG;
}
