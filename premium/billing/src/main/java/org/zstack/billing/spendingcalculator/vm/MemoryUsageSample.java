package org.zstack.billing.spendingcalculator.vm;

import org.zstack.billing.spendingcalculator.vm.VmUsageSample;

/**
 * Created by xing5 on 2016/6/7.
 */
public class MemoryUsageSample extends VmUsageSample {
    protected long memorySize;

    public long getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(long memorySize) {
        this.memorySize = memorySize;
    }
}
