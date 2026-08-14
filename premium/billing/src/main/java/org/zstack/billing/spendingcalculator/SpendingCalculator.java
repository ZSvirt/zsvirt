package org.zstack.billing.spendingcalculator;

import org.zstack.billing.Spending;
import org.zstack.billing.SpendingStruct;

/**
 * Created by frank on 3/4/2016.
 */
public interface SpendingCalculator {
    Spending calculate(SpendingStruct param);
}
