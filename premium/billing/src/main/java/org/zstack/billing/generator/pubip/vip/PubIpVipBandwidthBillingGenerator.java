package org.zstack.billing.generator.pubip.vip;

import org.zstack.billing.*;
import org.zstack.billing.generator.*;
import org.zstack.billing.spendingcalculator.SpendingCalculator;
import org.zstack.billing.spendingcalculator.vip.PubIpVipBandwidthSpending;
import org.zstack.billing.spendingcalculator.vip.PubIpVipBandwidthUsageVO;
import org.zstack.billing.spendingcalculator.vip.VipBandwidthSpendingCalculator;
import org.zstack.billing.spendingcalculator.vip.VipBandwidthSpendingDetails;
import org.zstack.core.db.Q;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.network.l2.L2NetworkClusterRefVO;
import org.zstack.header.network.l2.L2NetworkClusterRefVO_;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.kvm.KVMConstant;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.vip.VipVO_;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by lining on 2019/4/4.
 */
@SubBillingTypeConfig(type = BillingConstants.SPENDING_PUBLIC_IP_VIP_BANDWIDTH, subTypes = {BillingType.PubIpVipBandwidthIn, BillingType.PubIpVipBandwidthOut})
public class PubIpVipBandwidthBillingGenerator implements BillingGenerator {

    @Override
    public void generate(String accountUuid) {
        @BillingGeneratorConfig(usageClass = PubIpVipBandwidthUsageVO.class,usageResourceUuidFiledName = "vipUuid")
        class PubIpVipBandwidthBillingGeneratorTemplate extends BillingGeneratorTemplate {
            @Override
            protected List<String> getResourceNamesForPrice() {
                return list(BillingConstants.SPENDING_VIP_BANDWIDTH_IN, BillingConstants.SPENDING_VIP_BANDWIDTH_OUT);
            }

            @Override
            protected BillingType getBillingType() {
                return BillingType.PubIpVipBandwidth;
            }

            @Override
            protected UsageHistory make(Usage usage) {
                PubIpVipBandwidthUsageVO usageVO = (PubIpVipBandwidthUsageVO) usage;
                PubIpVipBandwidthUsageHistoryVO historyVO = new PubIpVipBandwidthUsageHistoryVO(usageVO);
                return historyVO;
            }

            @Override
            protected long getId(Usage usage) {
                PubIpVipBandwidthUsageVO usageVO = (PubIpVipBandwidthUsageVO) usage;
                return usageVO.getId();
            }

            @Override
            protected <T extends BillingVO> List<T> make(SpendingDetails spendingDetails) {
                PubIpVipBandwidthSpending spending = (PubIpVipBandwidthSpending) spendingDetails;

                List<PubIpVipBandwidthInBillingVO> bandwidthInBillingVOS = new ArrayList<>();
                List<PubIpVipBandwidthOutBillingVO> bandwidthOutBillingVOS = new ArrayList<>();

                if (spending.getBandwidthInInventory() != null) {
                    for (VipBandwidthSpendingDetails inventory : spending.getBandwidthInInventory()) {
                        PubIpVipBandwidthInBillingVO billingVO = new PubIpVipBandwidthInBillingVO();
                        billingVO.setBandwidthSize(inventory.bandwidthSize);
                        billingVO.setVipIp(spending.getVipIp());
                        billingVO.setResourceUuid(spending.resourceUuid);
                        billingVO.setAccountUuid(this.accountUuid);
                        billingVO.setResourceName(spending.getResourceName());
                        billingVO.setEndTime(inventory.endTime);
                        billingVO.setStartTime(inventory.startTime);
                        billingVO.setSpending(inventory.spending);
                        billingVO.setBillingType(BillingType.PubIpVipBandwidthIn);
                        billingVO.setHypervisorType(getHypervisorType(spending));
                        bandwidthInBillingVOS.add(billingVO);
                    }
                }

                if (spending.getBandwidthOutInventory() != null) {
                    for (VipBandwidthSpendingDetails inventory : spending.getBandwidthOutInventory()) {
                        PubIpVipBandwidthOutBillingVO billingVO = new PubIpVipBandwidthOutBillingVO();
                        billingVO.setBandwidthSize(inventory.bandwidthSize);
                        billingVO.setVipIp(spending.getVipIp());
                        billingVO.setResourceUuid(spending.resourceUuid);
                        billingVO.setAccountUuid(this.accountUuid);
                        billingVO.setResourceName(spending.getResourceName());
                        billingVO.setEndTime(inventory.endTime);
                        billingVO.setStartTime(inventory.startTime);
                        billingVO.setSpending(inventory.spending);
                        billingVO.setBillingType(BillingType.PubIpVipBandwidthOut);
                        billingVO.setHypervisorType(getHypervisorType(spending));
                        bandwidthOutBillingVOS.add(billingVO);
                    }
                }

                List<BillingVO> result = new ArrayList<>();
                result.addAll(bandwidthInBillingVOS);
                result.addAll(bandwidthOutBillingVOS);

                return (ArrayList<T>) result;
            }

            private String getHypervisorType(PubIpVipBandwidthSpending spending) {
                String hypervisorType = Q.New(BillingResourceLabelVO.class)
                        .eq(BillingResourceLabelVO_.resourceUuid, spending.resourceUuid)
                        .eq(BillingResourceLabelVO_.labelKey, BillingResourceLabelKey.HYPERVISORTYPE.toString())
                        .select(BillingResourceLabelVO_.labelValue)
                        .findValue();
                if (hypervisorType != null) {
                    return hypervisorType;
                }

                String l3NetworkUuid = Q.New(VipVO.class)
                        .eq(VipVO_.uuid, spending.resourceUuid)
                        .select(VipVO_.l3NetworkUuid)
                        .findValue();
                if (l3NetworkUuid == null) {
                    return KVMConstant.KVM_HYPERVISOR_TYPE;
                }

                String l2NetworkUuid = Q.New(L3NetworkVO.class)
                        .eq(L3NetworkVO_.uuid, l3NetworkUuid)
                        .select(L3NetworkVO_.l2NetworkUuid)
                        .findValue();

                String clusterUuid = Q.New(L2NetworkClusterRefVO.class)
                        .eq(L2NetworkClusterRefVO_.l2NetworkUuid, l2NetworkUuid)
                        .select(L2NetworkClusterRefVO_.clusterUuid)
                        .limit(1)
                        .findValue();
                if (clusterUuid == null) {
                    return KVMConstant.KVM_HYPERVISOR_TYPE;
                }

                hypervisorType = Q.New(ClusterVO.class)
                        .eq(ClusterVO_.uuid, clusterUuid)
                        .select(ClusterVO_.hypervisorType)
                        .findValue();
                return hypervisorType;
            }

            @Override
            protected List<Usage> makeNewUsageVOS(List<String> resourceUuids) {
                return new PubIpVipBandwidthUsageMaker().make(resourceUuids);
            }

            @Override
            protected SpendingCalculator getSpendingCalculator() {
                return new VipBandwidthSpendingCalculator();
            }
        }

        new PubIpVipBandwidthBillingGeneratorTemplate().generate(accountUuid);
    }

    @Override
    public List<String> getSpendingTypes() {
        return Arrays.asList(BillingConstants.SPENDING_VIP_BANDWIDTH_IN, BillingConstants.SPENDING_VIP_BANDWIDTH_OUT);
    }
}
