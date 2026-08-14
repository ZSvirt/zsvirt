package org.zstack.billing.generator.volume.root;

import org.zstack.billing.BillingConstants;
import org.zstack.billing.SpendingDetails;
import org.zstack.billing.Usage;
import org.zstack.billing.generator.*;
import org.zstack.billing.spendingcalculator.SpendingCalculator;
import org.zstack.billing.spendingcalculator.volume.root.*;
import org.zstack.core.db.Q;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by lining on 2019/4/3.
 */
@SubBillingTypeConfig(type = BillingConstants.SPENDING_TYPE_ROOT_VOLUME, subTypes = {BillingType.RootVolume})
public class RootVolumeBillingGenerator implements BillingGenerator {

    @Override
    public void generate(String accountUuid) {
        @BillingGeneratorConfig(usageClass = RootVolumeUsageVO.class, usageResourceUuidFiledName = "volumeUuid")
        class RootVolumeBillingGeneratorTemplate extends BillingGeneratorTemplate{
            @Override
            protected List<String> getResourceNamesForPrice() {
                return list(BillingConstants.SPENDING_ROOT_VOLUME);
            }

            @Override
            protected BillingType getBillingType() {
                return BillingType.RootVolume;
            }

            @Override
            protected UsageHistory make(Usage usage) {
                RootVolumeUsageVO usageVO = (RootVolumeUsageVO) usage;

                RootVolumeUsageExtensionVO extensionVO = Q.New(RootVolumeUsageExtensionVO.class)
                        .eq(RootVolumeUsageExtensionVO_.id, usageVO.getId())
                        .find();

                RootVolumeUsageHistoryVO historyVO = extensionVO != null ?
                        new RootVolumeUsageHistoryVO(extensionVO) :
                        new RootVolumeUsageHistoryVO(usageVO);
                return historyVO;
            }

            @Override
            protected long getId(Usage usage) {
                RootVolumeUsageVO usageVO = (RootVolumeUsageVO) usage;
                return usageVO.getId();
            }

            @Override
            protected <T extends BillingVO> List<T> make(SpendingDetails spendingDetails) {
                List<RootVolumeBillingVO> volumeBillingVOS = new ArrayList<>();

                RootVolumeSpending rootVolumeSpending = (RootVolumeSpending) spendingDetails;

                if (rootVolumeSpending.getSizeInventory() == null) {
                    return (ArrayList<T>) volumeBillingVOS;
                }

                for (RootVolumeSpendingInventory inventory : rootVolumeSpending.getSizeInventory()) {
                    RootVolumeBillingVO volumeBillingVO = new RootVolumeBillingVO();
                    volumeBillingVO.setResourceUuid(rootVolumeSpending.resourceUuid);
                    volumeBillingVO.setAccountUuid(this.accountUuid);
                    volumeBillingVO.setResourceName(rootVolumeSpending.getResourceName());
                    volumeBillingVO.setVolumeSize(inventory.volumeSize);
                    volumeBillingVO.setEndTime(inventory.endTime);
                    volumeBillingVO.setStartTime(inventory.startTime);
                    volumeBillingVO.setSpending(inventory.spending);
                    volumeBillingVO.setBillingType(this.getBillingType());
                    volumeBillingVO.setHypervisorType(rootVolumeSpending.hypervisorType);
                    volumeBillingVOS.add(volumeBillingVO);
                }

                return (ArrayList<T>) volumeBillingVOS;
            }

            @Override
            protected List<Usage> makeNewUsageVOS(List<String> resourceUuids) {
                return new RootVolumeUsageMaker().make(resourceUuids);
            }

            @Override
            protected SpendingCalculator getSpendingCalculator() {
                return new RootVolumeSpendingCalculator();
            }
        }

        new RootVolumeBillingGeneratorTemplate().generate(accountUuid);
    }

    @Override
    public List<String> getSpendingTypes() {
        return Collections.singletonList(BillingConstants.SPENDING_TYPE_ROOT_VOLUME);
    }
}
