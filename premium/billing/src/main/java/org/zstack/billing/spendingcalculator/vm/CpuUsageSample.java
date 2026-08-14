package org.zstack.billing.spendingcalculator.vm;

import org.zstack.billing.spendingcalculator.vm.VmUsageSample;

/**
 * Created by xing5 on 2016/6/7.
 */
public class CpuUsageSample extends VmUsageSample {
    protected int cpuNum;

    public int getCpuNum() {
        return cpuNum;
    }

    public void setCpuNum(int cpuNum) {
        this.cpuNum = cpuNum;
    }
}
