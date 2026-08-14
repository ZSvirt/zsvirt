package org.zstack.billing.generator.volume.data;

import org.zstack.billing.*;
import org.zstack.billing.generator.*;
import org.zstack.billing.spendingcalculator.SpendingCalculator;
import org.zstack.billing.spendingcalculator.volume.data.*;
import org.zstack.core.db.Q;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by lining on 2019/4/2.
 */
@SubBillingTypeConfig(type = BillingConstants.SPENDING_TYPE_DATA_VOLUME, subTypes = {BillingType.DataVolume})
public class DataVolumeBillingGenerator implements BillingGenerator {

    @Override
    public void generate(String accountUuid) {
        @BillingGeneratorConfig(usageClass = DataVolumeUsageVO.class, usageResourceUuidFiledName = "volumeUuid")
        class DataVolumeBillingGeneratorTemplate extends BillingGeneratorTemplate{
            @Override
            protected List<String> getResourceNamesForPrice() {
                return list(BillingConstants.SPENDING_DATA_VOLUME);
            }

            @Override
            protected BillingType getBillingType() {
                return BillingType.DataVolume;
            }

            @Override
            protected UsageHistory make(Usage usage) {
                DataVolumeUsageVO usageVO = (DataVolumeUsageVO) usage;

                DataVolumeUsageExtensionVO extensionVO = Q.New(DataVolumeUsageExtensionVO.class)
                        .eq(DataVolumeUsageHistoryVO_.id, usageVO.getId())
                        .find();

                DataVolumeUsageHistoryVO historyVO = extensionVO != null ?
                        new DataVolumeUsageHistoryVO(extensionVO) :
                        new DataVolumeUsageHistoryVO(usageVO);

                return historyVO;
            }

            @Override
            protected long getId(Usage usage) {
                DataVolumeUsageVO usageVO = (DataVolumeUsageVO) usage;
                return usageVO.getId();
            }

            @Override
            protected <T extends BillingVO> List<T> make(SpendingDetails spendingDetails) {
                List<DataVolumeBillingVO> volumeBillingVOS = new ArrayList<>();

                DataVolumeSpending dataVolumeSpending = (DataVolumeSpending) spendingDetails;

                if (dataVolumeSpending.getSizeInventory() == null) {
                    return (ArrayList<T>) volumeBillingVOS;
                }

                for (DataVolumeSpendingInventory inventory : dataVolumeSpending.getSizeInventory()) {
                    DataVolumeBillingVO volumeBillingVO = new DataVolumeBillingVO();
                    volumeBillingVO.setResourceUuid(dataVolumeSpending.resourceUuid);
                    volumeBillingVO.setAccountUuid(this.accountUuid);
                    volumeBillingVO.setResourceName(dataVolumeSpending.getResourceName());
                    volumeBillingVO.setVolumeSize(inventory.volumeSize);
                    volumeBillingVO.setEndTime(inventory.endTime);
                    volumeBillingVO.setStartTime(inventory.startTime);
                    volumeBillingVO.setSpending(inventory.spending);
                    volumeBillingVO.setBillingType(this.getBillingType());
                    volumeBillingVO.setHypervisorType(dataVolumeSpending.hypervisorType);
                    volumeBillingVOS.add(volumeBillingVO);
                }

                return (ArrayList<T>) volumeBillingVOS;
            }

            @Override
            protected List<Usage> makeNewUsageVOS(List<String> resourceUuids) {
                return new DataVolumeUsageMaker().make(resourceUuids);
            }

            @Override
            protected SpendingCalculator getSpendingCalculator() {
                return new VolumeSpendingCalculator();
            }
        }

        new DataVolumeBillingGeneratorTemplate().generate(accountUuid);
    }

    @Override
    public List<String> getSpendingTypes() {
        return Collections.singletonList(BillingConstants.SPENDING_TYPE_DATA_VOLUME);
    }
}
