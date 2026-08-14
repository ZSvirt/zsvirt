package org.zstack.billing.generator.pcidevice;

import org.zstack.billing.*;
import org.zstack.billing.generator.*;
import org.zstack.billing.spendingcalculator.SpendingCalculator;
import org.zstack.billing.spendingcalculator.pcidevice.PciDeviceSpending;
import org.zstack.billing.spendingcalculator.pcidevice.PciDeviceSpendingCalculator;
import org.zstack.billing.spendingcalculator.pcidevice.PciDeviceSpendingInventory;
import org.zstack.billing.spendingcalculator.pcidevice.PciDeviceUsageVO;
import org.zstack.core.db.Q;
import org.zstack.kvm.KVMConstant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;

@SubBillingTypeConfig(type = BillingConstants.SPENDING_TYPE_PCI_DEVICE, subTypes = {BillingType.GPU})
public class PciDeviceBillingGenerator implements BillingGenerator {

    @Override
    public void generate(String accountUuid) {

        @BillingGeneratorConfig(usageClass = PciDeviceUsageVO.class, usageResourceUuidFiledName = "pciDeviceUuid")
        class PciDeviceBillingGeneratorTemplate extends BillingGeneratorTemplate {

            @Override
            protected List<String> getResourceNamesForPrice() {
                return asList(BillingConstants.SPENDING_PCI_DEVICE);
            }

            @Override
            protected BillingType getBillingType() {
                return BillingType.GPU;
            }

            @Override
            protected UsageHistory make(Usage usage) {
                PciDeviceUsageVO usageVO = (PciDeviceUsageVO) usage;
                PciDeviceUsageHistoryVO historyVO = new PciDeviceUsageHistoryVO(usageVO);
                return historyVO;
            }

            @Override
            protected long getId(Usage usage) {
                PciDeviceUsageVO usageVO = (PciDeviceUsageVO) usage;
                return usageVO.getId();
            }

            @Override
            protected <T extends BillingVO> List<T> make(SpendingDetails spendingDetails) {
                List<PciDeviceBillingVO> pciDeviceBillingVOS = new ArrayList<>();

                PciDeviceSpending pciDeviceSpending = (PciDeviceSpending) spendingDetails;

                if (pciDeviceSpending.getSizeInventory() == null) {
                    return (ArrayList<T>) pciDeviceBillingVOS;
                }

                for (PciDeviceSpendingInventory inventory : pciDeviceSpending.getSizeInventory()) {
                    PciDeviceBillingVO pciDeviceBillingVO = new PciDeviceBillingVO();
                    pciDeviceBillingVO.setResourceUuid(pciDeviceSpending.resourceUuid);
                    pciDeviceBillingVO.setAccountUuid(this.accountUuid);
                    pciDeviceBillingVO.setResourceName(pciDeviceSpending.getResourceName());
                    pciDeviceBillingVO.setVmName(inventory.vmName);
                    pciDeviceBillingVO.setEndTime(inventory.endTime);
                    pciDeviceBillingVO.setStartTime(inventory.startTime);
                    pciDeviceBillingVO.setSpending(inventory.spending);
                    pciDeviceBillingVO.setBillingType(this.getBillingType());
                    pciDeviceBillingVO.setHypervisorType(getHypervisorType(pciDeviceSpending));
                    pciDeviceBillingVOS.add(pciDeviceBillingVO);
                }

                return (ArrayList<T>) pciDeviceBillingVOS;
            }

            private String getHypervisorType(PciDeviceSpending spending) {
                String hypervisorType = Q.New(BillingResourceLabelVO.class)
                        .eq(BillingResourceLabelVO_.resourceUuid, spending.resourceUuid)
                        .eq(BillingResourceLabelVO_.labelKey, BillingResourceLabelKey.HYPERVISORTYPE.toString())
                        .select(BillingResourceLabelVO_.labelValue)
                        .findValue();
                if (hypervisorType != null) {
                    return hypervisorType;
                }
                return KVMConstant.KVM_HYPERVISOR_TYPE;
            }

            @Override
            protected List<Usage> makeNewUsageVOS(List<String> resourceUuids) {
                return new PciDeviceUsageMaker().make(resourceUuids);
            }

            @Override
            protected SpendingCalculator getSpendingCalculator() {
                return new PciDeviceSpendingCalculator();
            }
        }

        new PciDeviceBillingGeneratorTemplate().generate(accountUuid);
    }

    @Override
    public List<String> getSpendingTypes() {
        return Collections.singletonList(BillingConstants.SPENDING_TYPE_PCI_DEVICE);
    }

}
