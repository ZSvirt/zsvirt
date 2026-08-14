package org.zstack.billing;

import java.util.List;

/**
 * Created by xing5 on 2016/6/7.
 */
public interface PriceCalculator {
    List<? extends UsageSample> calculate();
}
