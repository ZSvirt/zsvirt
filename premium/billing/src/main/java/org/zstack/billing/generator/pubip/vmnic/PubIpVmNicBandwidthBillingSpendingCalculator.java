package org.zstack.billing.generator.pubip.vmnic;

import org.zstack.billing.BillingConstants;
import org.zstack.billing.Spending;
import org.zstack.billing.SpendingDetails;
import org.zstack.billing.SpendingStruct;
import org.zstack.billing.generator.BillingSpendingCalculator;
import org.zstack.billing.generator.BillingSpendingCalculatorTemplate;
import org.zstack.billing.generator.BillingVO;
import org.zstack.billing.spendingcalculator.vmnic.PubIpVmNicBandwidthSpending;
import org.zstack.billing.spendingcalculator.vmnic.VmNicBandwidthSpendingDetails;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by lining on 2019/4/4.
 */
public class PubIpVmNicBandwidthBillingSpendingCalculator implements BillingSpendingCalculator {

    @Override
    public Spending calculate(SpendingStruct param) {
        return new BillingSpendingCalculatorTemplate() {
            @Override
            protected SpendingDetails make(BillingVO billingVO) {
                if (billingVO instanceof PubIpVmNicBandwidthInBillingVO) {
                    return make((PubIpVmNicBandwidthInBillingVO) billingVO);
                } else if (billingVO instanceof PubIpVmNicBandwidthOutBillingVO) {
                    return make((PubIpVmNicBandwidthOutBillingVO) billingVO);
                }

                return null;
            }

            private SpendingDetails make(PubIpVmNicBandwidthInBillingVO billingVO) {
                VmNicBandwidthSpendingDetails inventory = new VmNicBandwidthSpendingDetails();
                inventory.endTime = this.getSpendingEndTime(billingVO.getEndTime());
                inventory.startTime = this.getSpendingStartTime(billingVO.getStartTime());
                inventory.spending = this.reCalculationSpending(billingVO.getSpending(), billingVO.getStartTime(), billingVO.getEndTime());
                inventory.bandwidthSize = billingVO.getBandwidthSize();

                PubIpVmNicBandwidthSpending spendingDetails = new PubIpVmNicBandwidthSpending();
                spendingDetails.setSpending(inventory.spending);
                spendingDetails.setResourceName(billingVO.getResourceName());
                spendingDetails.setResourceUuid(billingVO.getResourceUuid());
                spendingDetails.setType(this.getSpendingType());
                spendingDetails.setVmNicIp(billingVO.getVmNicIp());
                spendingDetails.setHypervisorType(billingVO.getHypervisorType());

                if (!param.isSimple()) {
                    spendingDetails.setBandwidthInInventory(new ArrayList<>(Collections.singleton(inventory)));
                }
                return spendingDetails;
            }

            private SpendingDetails make(PubIpVmNicBandwidthOutBillingVO billingVO) {
                VmNicBandwidthSpendingDetails inventory = new VmNicBandwidthSpendingDetails();
                inventory.endTime = this.getSpendingEndTime(billingVO.getEndTime());
                inventory.startTime = this.getSpendingStartTime(billingVO.getStartTime());
                inventory.spending = this.reCalculationSpending(billingVO.getSpending(), billingVO.getStartTime(), billingVO.getEndTime());
                inventory.bandwidthSize = billingVO.getBandwidthSize();

                PubIpVmNicBandwidthSpending spendingDetails = new PubIpVmNicBandwidthSpending();
                spendingDetails.setSpending(inventory.spending);
                spendingDetails.setResourceName(billingVO.getResourceName());
                spendingDetails.setResourceUuid(billingVO.getResourceUuid());
                spendingDetails.setType(this.getSpendingType());
                spendingDetails.setHypervisorType(billingVO.getHypervisorType());

                if (!param.isSimple()) {
                    spendingDetails.setBandwidthOutInventory(new ArrayList<>(Collections.singleton(inventory)));
                }
                return spendingDetails;
            }

            @Override
            protected void merge(SpendingDetails source, SpendingDetails target) {
                PubIpVmNicBandwidthSpending sourceSpending = (PubIpVmNicBandwidthSpending) source;
                PubIpVmNicBandwidthSpending targetSpending = (PubIpVmNicBandwidthSpending) target;

                if (targetSpending.getBandwidthInInventory() != null) {
                    if (sourceSpending.getBandwidthInInventory() == null) {
                        sourceSpending.setBandwidthInInventory(targetSpending.getBandwidthInInventory());
                    } else {
                        sourceSpending.getBandwidthInInventory().addAll(targetSpending.getBandwidthInInventory());
                    }
                }

                if (targetSpending.getBandwidthOutInventory() != null) {
                    if (sourceSpending.getBandwidthOutInventory() == null) {
                        sourceSpending.setBandwidthOutInventory(targetSpending.getBandwidthOutInventory());
                    } else {
                        sourceSpending.getBandwidthOutInventory().addAll(targetSpending.getBandwidthOutInventory());
                    }
                }
            }

            @Override
            protected String getSpendingType() {
                return BillingConstants.SPENDING_PUBLIC_IP_VM_NIC_BANDWIDTH;
            }

            @Override
            protected int getBillingCount() {
                return getBillingVOCount(PubIpVmNicBandwidthInBillingVO.class) + getBillingVOCount(PubIpVmNicBandwidthOutBillingVO.class);
            }

            @Override
            protected List<BillingVO> getBillingVOS(int offset) {
                List<BillingVO> result = new ArrayList<>();

                List<BillingVO> subBillingVOS = this.getBillingVOS(PubIpVmNicBandwidthInBillingVO.class, offset);
                result.addAll(subBillingVOS);

                subBillingVOS = this.getBillingVOS(PubIpVmNicBandwidthOutBillingVO.class, offset);
                result.addAll(subBillingVOS);

                return result;
            }
        }.calculate(param);
    }
}
