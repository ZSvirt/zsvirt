package org.zstack.billing.generator.vm;

import org.zstack.billing.BillingConstants;
import org.zstack.billing.Spending;
import org.zstack.billing.SpendingDetails;
import org.zstack.billing.SpendingStruct;
import org.zstack.billing.generator.BillingSpendingCalculator;
import org.zstack.billing.generator.BillingSpendingCalculatorTemplate;
import org.zstack.billing.generator.BillingVO;
import org.zstack.billing.generator.vm.cpu.VmCPUBillingVO;
import org.zstack.billing.generator.vm.memory.VmMemoryBillingVO;
import org.zstack.billing.spendingcalculator.vm.VmSpending;
import org.zstack.billing.spendingcalculator.vm.VmSpendingDetails;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by lining on 2019/4/4.
 */
public class VmBillingSpendingCalculator implements BillingSpendingCalculator {

    @Override
    public Spending calculate(SpendingStruct param) {
        return new BillingSpendingCalculatorTemplate() {
            @Override
            protected SpendingDetails make(BillingVO billingVO) {
                if (billingVO instanceof VmCPUBillingVO) {
                    return make((VmCPUBillingVO) billingVO);
                } else if (billingVO instanceof VmMemoryBillingVO) {
                    return make((VmMemoryBillingVO) billingVO);
                }

                return null;
            }

            private SpendingDetails make(VmCPUBillingVO billingVO) {
                VmSpendingDetails inventory = new VmSpendingDetails();
                inventory.endTime = this.getSpendingEndTime(billingVO.getEndTime());
                inventory.startTime = this.getSpendingStartTime(billingVO.getStartTime());
                inventory.spending = this.reCalculationSpending(billingVO.getSpending(), billingVO.getStartTime(), billingVO.getEndTime());

                VmSpending spendingDetails = new VmSpending();
                spendingDetails.setSpending(inventory.spending);
                spendingDetails.setResourceName(billingVO.getResourceName());
                spendingDetails.setResourceUuid(billingVO.getResourceUuid());
                spendingDetails.setType(this.getSpendingType());
                spendingDetails.setHypervisorType(billingVO.getHypervisorType());

                if (!param.isSimple()) {
                    spendingDetails.setCpuInventory(new ArrayList<>(Collections.singleton(inventory)));
                }
                return spendingDetails;
            }

            private SpendingDetails make(VmMemoryBillingVO billingVO) {
                VmSpendingDetails inventory = new VmSpendingDetails();
                inventory.endTime = this.getSpendingEndTime(billingVO.getEndTime());
                inventory.startTime = this.getSpendingStartTime(billingVO.getStartTime());
                inventory.spending = this.reCalculationSpending(billingVO.getSpending(), billingVO.getStartTime(), billingVO.getEndTime());

                VmSpending spendingDetails = new VmSpending();
                spendingDetails.setSpending(inventory.spending);
                spendingDetails.setResourceName(billingVO.getResourceName());
                spendingDetails.setResourceUuid(billingVO.getResourceUuid());
                spendingDetails.setType(this.getSpendingType());
                spendingDetails.setHypervisorType(billingVO.getHypervisorType());

                if (!param.isSimple()) {
                    spendingDetails.setMemoryInventory(new ArrayList<>(Collections.singleton(inventory)));
                }
                return spendingDetails;
            }

            @Override
            protected void merge(SpendingDetails source, SpendingDetails target) {
                VmSpending sourceSpending = (VmSpending) source;
                VmSpending targetSpending = (VmSpending) target;

                if (targetSpending.getCpuInventory() != null) {
                    if (sourceSpending.getCpuInventory() == null) {
                        sourceSpending.setCpuInventory(targetSpending.getCpuInventory());
                    } else {
                        sourceSpending.getCpuInventory().addAll(targetSpending.getCpuInventory());
                    }
                }

                if (targetSpending.getMemoryInventory() != null) {
                    if (sourceSpending.getMemoryInventory() == null) {
                        sourceSpending.setMemoryInventory(targetSpending.getMemoryInventory());
                    } else {
                        sourceSpending.getMemoryInventory().addAll(targetSpending.getMemoryInventory());
                    }
                }
            }

            @Override
            protected String getSpendingType() {
                return BillingConstants.SPENDING_TYPE_VM;
            }

            @Override
            protected int getBillingCount() {
                return this.getBillingVOCount(VmCPUBillingVO.class) + this.getBillingVOCount(VmMemoryBillingVO.class);
            }

            @Override
            protected List<BillingVO> getBillingVOS(int offset) {
                List<BillingVO> result = new ArrayList<>();

                List<BillingVO> cpuBillingVOS = this.getBillingVOS(VmCPUBillingVO.class, offset);
                result.addAll(cpuBillingVOS);

                List<BillingVO> memoryBillingVOS = this.getBillingVOS(VmMemoryBillingVO.class, offset);
                result.addAll(memoryBillingVOS);

                return result;
            }
        }.calculate(param);
    }
}
