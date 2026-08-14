package org.zstack.billing.generator.vm;

import org.zstack.billing.*;
import org.zstack.billing.generator.*;
import org.zstack.billing.generator.vm.cpu.VmCPUBillingVO;
import org.zstack.billing.generator.vm.memory.VmMemoryBillingVO;
import org.zstack.billing.spendingcalculator.SpendingCalculator;
import org.zstack.billing.spendingcalculator.vm.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by lining on 2019/4/3.
 */
@SubBillingTypeConfig(type = BillingConstants.SPENDING_TYPE_VM, subTypes = {BillingType.CPU, BillingType.Memory})
public class VmBillingGenerator implements BillingGenerator {

    @Override
    public void generate(String accountUuid) {
        @BillingGeneratorConfig(usageClass = VmUsageVO.class, usageResourceUuidFiledName = "vmUuid")
        class VmBillingGeneratorTemplate extends BillingGeneratorTemplate {
            @Override
            protected List<String> getResourceNamesForPrice() {
                return asList(BillingConstants.SPENDING_CPU, BillingConstants.SPENDING_MEMORY);
            }

            @Override
            protected BillingType getBillingType() {
                return BillingType.Vm;
            }

            @Override
            protected UsageHistory make(Usage usage) {
                VmUsageVO usageVO = (VmUsageVO) usage;
                VmUsageHistoryVO historyVO = new VmUsageHistoryVO(usageVO);
                return historyVO;
            }

            @Override
            protected long getId(Usage usage) {
                VmUsageVO usageVO = (VmUsageVO) usage;
                return usageVO.getId();
            }

            @Override
            protected <T extends BillingVO> List<T> make(SpendingDetails spendingDetails) {
                List<VmCPUBillingVO> cpuBillingVOS = new ArrayList<>();
                List<VmMemoryBillingVO> memoryBillingVOS = new ArrayList<>();

                VmSpending vmSpending = (VmSpending) spendingDetails;

                if (vmSpending.getCpuInventory() != null) {
                    for (VmSpendingDetails inventory : vmSpending.getCpuInventory()) {
                        VmCPUSpendingDetails cpuInventory = (VmCPUSpendingDetails)inventory;
                        VmCPUBillingVO billingVO = new VmCPUBillingVO();
                        billingVO.setResourceUuid(vmSpending.resourceUuid);
                        billingVO.setAccountUuid(this.accountUuid);
                        billingVO.setResourceName(vmSpending.getResourceName());
                        billingVO.setEndTime(cpuInventory.endTime);
                        billingVO.setStartTime(cpuInventory.startTime);
                        billingVO.setSpending(cpuInventory.spending);
                        billingVO.setBillingType(BillingType.CPU);
                        billingVO.setHypervisorType(vmSpending.hypervisorType);
                        billingVO.setCpuNum(cpuInventory.cpuNum);
                        cpuBillingVOS.add(billingVO);
                    }
                }

                if (vmSpending.getMemoryInventory() != null) {
                    for (VmSpendingDetails inventory : vmSpending.getMemoryInventory()) {
                        VmMemorySpendingDetails memoryInventory = (VmMemorySpendingDetails)inventory;
                        VmMemoryBillingVO billingVO = new VmMemoryBillingVO();
                        billingVO.setResourceUuid(vmSpending.resourceUuid);
                        billingVO.setAccountUuid(this.accountUuid);
                        billingVO.setResourceName(vmSpending.getResourceName());
                        billingVO.setEndTime(memoryInventory.endTime);
                        billingVO.setStartTime(memoryInventory.startTime);
                        billingVO.setSpending(memoryInventory.spending);
                        billingVO.setBillingType(BillingType.Memory);
                        billingVO.setHypervisorType(vmSpending.hypervisorType);
                        billingVO.setMemorySize(memoryInventory.memorySize);
                        memoryBillingVOS.add(billingVO);
                    }
                }

                List<BillingVO> result = new ArrayList<>();
                result.addAll(cpuBillingVOS);
                result.addAll(memoryBillingVOS);

                return (ArrayList<T>) result;
            }

            @Override
            protected List<Usage> makeNewUsageVOS(List<String> resourceUuids) {
                return new VmUsageMaker().make(resourceUuids);
            }

            @Override
            protected SpendingCalculator getSpendingCalculator() {
                return new VmSpendingCalculator();
            }
        }

        new VmBillingGeneratorTemplate().generate(accountUuid);
    }

    @Override
    public List<String> getSpendingTypes() {
        return Arrays.asList(BillingConstants.SPENDING_CPU, BillingConstants.SPENDING_MEMORY);
    }
}
