package org.zstack.billing.generator.pubip.vip;

import org.zstack.billing.BillingConstants;
import org.zstack.billing.Spending;
import org.zstack.billing.SpendingDetails;
import org.zstack.billing.SpendingStruct;
import org.zstack.billing.generator.BillingSpendingCalculator;
import org.zstack.billing.generator.BillingSpendingCalculatorTemplate;
import org.zstack.billing.generator.BillingVO;
import org.zstack.billing.spendingcalculator.vip.PubIpVipBandwidthSpending;
import org.zstack.billing.spendingcalculator.vip.VipBandwidthSpendingDetails;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by lining on 2019/4/4.
 */
public class PubIpVipBandwidthBillingSpendingCalculator implements BillingSpendingCalculator {
    @Override
    public Spending calculate(SpendingStruct param) {
        return new BillingSpendingCalculatorTemplate() {
            @Override
            protected SpendingDetails make(BillingVO billingVO) {
                if (billingVO instanceof PubIpVipBandwidthInBillingVO) {
                    return make((PubIpVipBandwidthInBillingVO) billingVO);
                } else if (billingVO instanceof PubIpVipBandwidthOutBillingVO) {
                    return make((PubIpVipBandwidthOutBillingVO) billingVO);
                }

                return null;
            }

            private SpendingDetails make(PubIpVipBandwidthInBillingVO billingVO) {
                VipBandwidthSpendingDetails inventory = new VipBandwidthSpendingDetails();
                inventory.endTime = this.getSpendingEndTime(billingVO.getEndTime());
                inventory.startTime = this.getSpendingStartTime(billingVO.getStartTime());
                inventory.spending = this.reCalculationSpending(billingVO.getSpending(), billingVO.getStartTime(), billingVO.getEndTime());
                inventory.bandwidthSize = billingVO.getBandwidthSize();

                PubIpVipBandwidthSpending spendingDetails = new PubIpVipBandwidthSpending();
                spendingDetails.setSpending(inventory.spending);
                spendingDetails.setResourceName(billingVO.getResourceName());
                spendingDetails.setResourceUuid(billingVO.getResourceUuid());
                spendingDetails.setType(this.getSpendingType());
                spendingDetails.setVipIp(billingVO.getVipIp());
                spendingDetails.setHypervisorType(billingVO.getHypervisorType());

                if (!param.isSimple()) {
                    spendingDetails.setBandwidthInInventory(new ArrayList<>(Collections.singleton(inventory)));
                }
                return spendingDetails;
            }

            private SpendingDetails make(PubIpVipBandwidthOutBillingVO billingVO) {
                VipBandwidthSpendingDetails inventory = new VipBandwidthSpendingDetails();
                inventory.endTime = this.getSpendingEndTime(billingVO.getEndTime());
                inventory.startTime = this.getSpendingStartTime(billingVO.getStartTime());
                inventory.spending = this.reCalculationSpending(billingVO.getSpending(), billingVO.getStartTime(), billingVO.getEndTime());
                inventory.bandwidthSize = billingVO.getBandwidthSize();

                PubIpVipBandwidthSpending spendingDetails = new PubIpVipBandwidthSpending();
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
                PubIpVipBandwidthSpending sourceSpending = (PubIpVipBandwidthSpending) source;
                PubIpVipBandwidthSpending targetSpending = (PubIpVipBandwidthSpending) target;

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
                return BillingConstants.SPENDING_PUBLIC_IP_VIP_BANDWIDTH;
            }

            @Override
            protected int getBillingCount() {
                return this.getBillingVOCount(PubIpVipBandwidthInBillingVO.class) + this.getBillingVOCount(PubIpVipBandwidthOutBillingVO.class);
            }

            @Override
            protected List<BillingVO> getBillingVOS(int offset) {
                List<BillingVO> result = new ArrayList<>();

                List<BillingVO> subBillingVOS = this.getBillingVOS(PubIpVipBandwidthInBillingVO.class, offset);
                result.addAll(subBillingVOS);

                subBillingVOS = this.getBillingVOS(PubIpVipBandwidthOutBillingVO.class, offset);
                result.addAll(subBillingVOS);

                return result;
            }
        }.calculate(param);
    }
}
