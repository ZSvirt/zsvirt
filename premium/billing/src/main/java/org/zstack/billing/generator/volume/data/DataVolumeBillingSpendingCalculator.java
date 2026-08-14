package org.zstack.billing.generator.volume.data;

import org.zstack.billing.*;
import org.zstack.billing.generator.BillingSpendingCalculator;
import org.zstack.billing.generator.BillingSpendingCalculatorTemplate;
import org.zstack.billing.generator.BillingVO;
import org.zstack.billing.spendingcalculator.volume.data.DataVolumeSpending;
import org.zstack.billing.spendingcalculator.volume.data.DataVolumeSpendingInventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by lining on 2019/4/4.
 */
public class DataVolumeBillingSpendingCalculator implements BillingSpendingCalculator  {
    @Override
    public Spending calculate(SpendingStruct param) {
        return new BillingSpendingCalculatorTemplate() {
            @Override
            protected SpendingDetails make(BillingVO billingVO) {
                DataVolumeBillingVO volumeBillingVO = (DataVolumeBillingVO) billingVO;

                DataVolumeSpendingInventory inventory = new DataVolumeSpendingInventory();
                inventory.endTime = this.getSpendingEndTime(billingVO.getEndTime());
                inventory.startTime = this.getSpendingStartTime(billingVO.getStartTime());
                inventory.spending = this.reCalculationSpending(volumeBillingVO.getSpending(), volumeBillingVO.getStartTime(), volumeBillingVO.getEndTime());
                inventory.volumeSize = volumeBillingVO.getVolumeSize();

                DataVolumeSpending spendingDetails = new DataVolumeSpending();
                spendingDetails.setSpending(inventory.spending);
                spendingDetails.setResourceName(volumeBillingVO.getResourceName());
                spendingDetails.setResourceUuid(volumeBillingVO.getResourceUuid());
                spendingDetails.setType(this.getSpendingType());
                spendingDetails.setHypervisorType(volumeBillingVO.getHypervisorType());

                if (!param.isSimple()) {
                    spendingDetails.setSizeInventory(new ArrayList<>(Collections.singleton(inventory)));
                }
                return spendingDetails;
            }

            @Override
            protected void merge(SpendingDetails source, SpendingDetails target) {
                DataVolumeSpending sourceSpending = (DataVolumeSpending) source;
                DataVolumeSpending targetSpending = (DataVolumeSpending) target;

                sourceSpending.getSizeInventory().addAll(targetSpending.getSizeInventory());
            }

            @Override
            protected String getSpendingType() {
                return BillingConstants.SPENDING_TYPE_DATA_VOLUME;
            }

            @Override
            protected int getBillingCount() {
                return this.getBillingVOCount(DataVolumeBillingVO.class);
            }

            @Override
            protected List<BillingVO> getBillingVOS(int offset) {
                return this.getBillingVOS(DataVolumeBillingVO.class, offset);
            }
        }.calculate(param);
    }
}
