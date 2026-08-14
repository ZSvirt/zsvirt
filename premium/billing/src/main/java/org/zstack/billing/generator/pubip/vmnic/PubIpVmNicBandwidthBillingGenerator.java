package org.zstack.billing.generator.pubip.vmnic;

import org.zstack.billing.*;
import org.zstack.billing.generator.*;
import org.zstack.billing.spendingcalculator.SpendingCalculator;
import org.zstack.billing.spendingcalculator.vmnic.*;
import org.zstack.core.db.Q;
import org.zstack.header.vm.*;
import org.zstack.kvm.KVMConstant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by lining on 2019/4/4.
 */
@SubBillingTypeConfig(type = BillingConstants.SPENDING_PUBLIC_IP_VM_NIC_BANDWIDTH, subTypes = {BillingType.PubIpVmNicBandwidthIn, BillingType.PubIpVmNicBandwidthOut})
public class PubIpVmNicBandwidthBillingGenerator implements BillingGenerator {

    @Override
    public void generate(String accountUuid) {
        @BillingGeneratorConfig(usageClass = PubIpVmNicBandwidthUsageVO.class, usageResourceUuidFiledName = "vmNicUuid")
        class  PubIpVmNicBandwidthBillingGeneratorTemplate extends BillingGeneratorTemplate {
            @Override
            protected List<String> getResourceNamesForPrice() {
                return list(BillingConstants.SPENDING_VM_NIC_BANDWIDTH_IN, BillingConstants.SPENDING_VM_NIC_BANDWIDTH_OUT);
            }

            @Override
            protected BillingType getBillingType() {
                return BillingType.PubIpVmNicBandwidth;
            }

            @Override
            protected UsageHistory make(Usage usage) {
                PubIpVmNicBandwidthUsageVO usageVO = (PubIpVmNicBandwidthUsageVO) usage;
                PubIpVmNicBandwidthUsageHistoryVO historyVO = new PubIpVmNicBandwidthUsageHistoryVO(usageVO);
                return historyVO;
            }

            @Override
            protected long getId(Usage usage) {
                PubIpVmNicBandwidthUsageVO usageVO = (PubIpVmNicBandwidthUsageVO) usage;
                return usageVO.getId();
            }

            @Override
            protected <T extends BillingVO> List<T> make(SpendingDetails spendingDetails) {
                PubIpVmNicBandwidthSpending spending = (PubIpVmNicBandwidthSpending) spendingDetails;

                List<PubIpVmNicBandwidthInBillingVO> bandwidthInBillingVOS = new ArrayList<>();
                List<PubIpVmNicBandwidthOutBillingVO> bandwidthOutBillingVOS = new ArrayList<>();

                if (spending.getBandwidthInInventory() != null) {
                    for (VmNicBandwidthSpendingDetails inventory : spending.getBandwidthInInventory()) {
                        PubIpVmNicBandwidthInBillingVO billingVO = new PubIpVmNicBandwidthInBillingVO();
                        billingVO.setBandwidthSize(inventory.bandwidthSize);
                        billingVO.setVmNicIp(spending.getVmNicIp());
                        billingVO.setResourceUuid(spending.resourceUuid);
                        billingVO.setAccountUuid(this.accountUuid);
                        billingVO.setResourceName(spending.getResourceName());
                        billingVO.setEndTime(inventory.endTime);
                        billingVO.setStartTime(inventory.startTime);
                        billingVO.setSpending(inventory.spending);
                        billingVO.setBillingType(BillingType.PubIpVmNicBandwidthIn);
                        billingVO.setHypervisorType(getHypervisorType(spending));
                        bandwidthInBillingVOS.add(billingVO);
                    }
                }

                if (spending.getBandwidthOutInventory() != null) {
                    for (VmNicBandwidthSpendingDetails inventory : spending.getBandwidthOutInventory()) {
                        PubIpVmNicBandwidthOutBillingVO billingVO = new PubIpVmNicBandwidthOutBillingVO();
                        billingVO.setBandwidthSize(inventory.bandwidthSize);
                        billingVO.setVmNicIp(spending.getVmNicIp());
                        billingVO.setResourceUuid(spending.resourceUuid);
                        billingVO.setAccountUuid(this.accountUuid);
                        billingVO.setResourceName(spending.getResourceName());
                        billingVO.setEndTime(inventory.endTime);
                        billingVO.setStartTime(inventory.startTime);
                        billingVO.setSpending(inventory.spending);
                        billingVO.setBillingType(BillingType.PubIpVmNicBandwidthOut);
                        billingVO.setHypervisorType(getHypervisorType(spending));
                        bandwidthOutBillingVOS.add(billingVO);
                    }
                }

                List<BillingVO> result = new ArrayList<>();
                result.addAll(bandwidthInBillingVOS);
                result.addAll(bandwidthOutBillingVOS);

                return (ArrayList<T>) result;
            }

            private String getHypervisorType(PubIpVmNicBandwidthSpending spending) {
                String hypervisorType = Q.New(BillingResourceLabelVO.class)
                        .eq(BillingResourceLabelVO_.resourceUuid, spending.resourceUuid)
                        .eq(BillingResourceLabelVO_.labelKey, BillingResourceLabelKey.HYPERVISORTYPE.toString())
                        .select(BillingResourceLabelVO_.labelValue)
                        .findValue();
                if (hypervisorType != null) {
                    return hypervisorType;
                }

                String vmUuid = Q.New(VmNicVO.class)
                        .eq(VmNicVO_.uuid, spending.resourceUuid)
                        .select(VmNicVO_.vmInstanceUuid)
                        .findValue();
                if (vmUuid == null) {
                    return KVMConstant.KVM_HYPERVISOR_TYPE;
                }

                hypervisorType = Q.New(VmInstanceVO.class)
                        .eq(VmInstanceVO_.uuid, vmUuid)
                        .select(VmInstanceVO_.hypervisorType)
                        .findValue();
                return hypervisorType;
            }

            @Override
            protected List<Usage> makeNewUsageVOS(List<String> resourceUuids) {
                return new PubIpVmNicBandwidthUsageMaker().make(resourceUuids);
            }

            @Override
            protected SpendingCalculator getSpendingCalculator() {
                return new VmNicBandwidthSpendingCalculator();
            }
        }

        new PubIpVmNicBandwidthBillingGeneratorTemplate().generate(accountUuid);
    }

    @Override
    public List<String> getSpendingTypes() {
        return Arrays.asList(BillingConstants.SPENDING_VM_NIC_BANDWIDTH_IN, BillingConstants.SPENDING_VM_NIC_BANDWIDTH_OUT);
    }
}
