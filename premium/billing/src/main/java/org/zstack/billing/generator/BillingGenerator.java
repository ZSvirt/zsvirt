package org.zstack.billing.generator;

import java.util.List;

/**
 * Created by lining on 2019/4/1.
 */
public interface BillingGenerator {
    void generate(String accountUuid);

    List<String> getSpendingTypes();
}
