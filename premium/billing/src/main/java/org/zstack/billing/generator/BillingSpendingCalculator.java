package org.zstack.billing.generator;

import org.zstack.billing.Spending;
import org.zstack.billing.SpendingStruct;

/**
 * Created by lining on 2019/4/4.
 */
public interface BillingSpendingCalculator {
    Spending calculate(SpendingStruct param);
}
