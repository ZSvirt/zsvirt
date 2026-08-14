package org.zstack.billing.generator.pcidevice;

import org.zstack.billing.*;
import org.zstack.billing.generator.BillingSpendingCalculator;
import org.zstack.billing.generator.BillingSpendingCalculatorTemplate;
import org.zstack.billing.generator.BillingVO;
import org.zstack.billing.spendingcalculator.pcidevice.PciDeviceSpending;
import org.zstack.billing.spendingcalculator.pcidevice.PciDeviceSpendingInventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PciDeviceBillingSpendingCalculator implements BillingSpendingCalculator {
    @Override
    public Spending calculate(SpendingStruct param) {
        return new BillingSpendingCalculatorTemplate() {
            @Override
            protected SpendingDetails make(BillingVO billingVO) {
                PciDeviceBillingVO pciDeviceBillingVO = (PciDeviceBillingVO) billingVO;

                PciDeviceSpendingInventory inventory = new PciDeviceSpendingInventory();
                inventory.endTime = this.getSpendingEndTime(billingVO.getEndTime());
                inventory.startTime = this.getSpendingStartTime(billingVO.getStartTime());
                inventory.spending = this.reCalculationSpending(pciDeviceBillingVO.getSpending(), billingVO.getStartTime(), billingVO.getEndTime());
                inventory.vmName = pciDeviceBillingVO.getVmName();

                PciDeviceSpending spendingDetails = new PciDeviceSpending();
                spendingDetails.setSpending(inventory.spending);
                spendingDetails.setResourceName(pciDeviceBillingVO.getResourceName());
                spendingDetails.setResourceUuid(pciDeviceBillingVO.getResourceUuid());
                spendingDetails.setType(this.getSpendingType());
                spendingDetails.setHypervisorType(pciDeviceBillingVO.getHypervisorType());


                if (!param.isSimple()) {
                    spendingDetails.setSizeInventory(new ArrayList<>(Collections.singleton(inventory)));
                }
                return spendingDetails;
            }

            @Override
            protected void merge(SpendingDetails source, SpendingDetails target) {
                PciDeviceSpending sourceSpending = (PciDeviceSpending) source;
                PciDeviceSpending targetSpending = (PciDeviceSpending) target;

                sourceSpending.getSizeInventory().addAll(targetSpending.getSizeInventory());
            }

            @Override
            protected String getSpendingType() {
                return BillingConstants.SPENDING_PCI_DEVICE;
            }

            @Override
            protected int getBillingCount() {
                return this.getBillingVOCount(PciDeviceBillingVO.class);
            }

            @Override
            protected List<BillingVO> getBillingVOS(int offset) {
                return this.getBillingVOS(PciDeviceBillingVO.class, offset);
            }
        }.calculate(param);
    }
}
