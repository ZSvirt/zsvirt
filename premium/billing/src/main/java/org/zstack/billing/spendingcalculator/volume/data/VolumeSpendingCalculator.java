package org.zstack.billing.spendingcalculator.volume.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.billing.*;
import org.zstack.billing.spendingcalculator.SpendingCalculator;
import org.zstack.billing.userconfig.BillingUserConfigUtils;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.KVMConstant;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionDSL.*;

/**
 * Created by xing5 on 2016/3/12.
 */
public class VolumeSpendingCalculator implements SpendingCalculator {
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public Spending calculate(final SpendingStruct param) {
        return new BillingTemplate(param) {
            @Override
            protected Class getUsageClass() {
                return DataVolumeUsageVO.class;
            }

            @Override
            protected String getUsageResourceUuidFiledName() {
                return "volumeUuid";
            }

            @Override
            protected List<String> getResourceNamesForPrice() {
                return list(BillingConstants.SPENDING_DATA_VOLUME);
            }

            @Override
            protected String getSpendingType() {
                return BillingConstants.SPENDING_TYPE_DATA_VOLUME;
            }

            @Override
            protected RangeOp returnRangeOp(List cache, Usage co) {
                DataVolumeUsageVO d = (DataVolumeUsageVO) co;
                if (VolumeStatus.Ready.toString().equals(d.getVolumeStatus())) {
                    if (cache.isEmpty()) {
                        return RangeOp.ADD_TO_CACHE;
                    }
                    List<DataVolumeUsageVO> usageVOS = cache;
                    DataVolumeUsageVO lastVO = usageVOS.get(usageVOS.size() - 1);
                    if (lastVO.getVolumeSize() == d.getVolumeSize()) {
                        return RangeOp.ADD_TO_CACHE;
                    } else {
                        return RangeOp.CLOSE_AND_ADD_TO_CACHE;
                    }
                } else if (!cache.isEmpty() && VolumeStatus.Deleted.toString().equals(d.getVolumeStatus())) {
                    return RangeOp.CLOSE;
                } else {
                    return RangeOp.SKIP;
                }
            }

            @Override
            protected List<SampleBundle> generateSampleBundle(ChargeRange range) {
                DataVolumeUsageVO d = (DataVolumeUsageVO) range.getUsageCO();
                SampleBundle bundle = new SampleBundle();
                bundle.resourceName = BillingConstants.SPENDING_DATA_VOLUME;
                String hypervisorType;

                if (d.getInventory() == null) {
                    hypervisorType = KVMConstant.KVM_HYPERVISOR_TYPE;
                }else {
                    String format = JSONObjectUtil.toObject(d.getInventory(), VolumeInventory.class).getFormat();
                    hypervisorType = KVMConstant.KVM_HYPERVISOR_TYPE;
                }

                DataVolumeUsageSample s = new DataVolumeUsageSample();
                s.setAccountUuid(d.getAccountUuid());
                s.setVolumeName(d.getVolumeName());
                s.setVolumeUuid(d.getVolumeUuid());
                s.setUsage(d.getVolumeSize());
                s.setHypervisorType(hypervisorType);

                DataVolumeUsageExtensionVO extensionVO = Q.New(DataVolumeUsageExtensionVO.class)
                        .eq(DataVolumeUsageExtensionVO_.id, d.getId())
                        .find();
                if (extensionVO != null ) {
                    s.setResourcePriceUserConfig(extensionVO.getResourcePriceUserConfig());
                }
                bundle.sample = s;
                return list(bundle);
            }

            @Override
            protected Map<String, List<UsageSample>> evaluateSampleSpending(Map<String, List<UsageSample>> samples, List<PriceVO> prices) {
                Map<String, List<PriceVO>> priceMap = new HashMap<>();
                for (PriceVO co : prices) {
                    String priceUserConfig = null;

                    if (BillingSystemTags.PRICE_USER_CONFIG.hasTag(co.getUuid())) {
                        priceUserConfig = BillingUserConfigUtils.getResourcePriceConfig(co.getUuid()).getPriceUserConfig().getPriceKeyName();
                    }

                    List<PriceVO> priceList = priceMap.computeIfAbsent(priceUserConfig, k -> new ArrayList<>());
                    priceList.add(co);
                }

                Map<String, List<UsageSample>> sampleMap = new HashMap<>();
                for (UsageSample sample: samples.get(BillingConstants.SPENDING_DATA_VOLUME)) {
                    DataVolumeUsageSample usageSample = (DataVolumeUsageSample) sample;
                    String priceUserConfig = usageSample.getResourcePriceUserConfig();

                    List<UsageSample> sampleList = sampleMap.computeIfAbsent(priceUserConfig, k -> new ArrayList<>());
                    sampleList.add(sample);
                }

                List ret = new ArrayList<>();
                for (Map.Entry<String, List<PriceVO>> e: priceMap.entrySet()) {
                    String priceUserConfig = e.getKey();
                    List<UsageSample> usageSamples = sampleMap.get(priceUserConfig);
                    if (usageSamples == null) {
                        continue;
                    }

                    List<PriceVO> priceVOS = e.getValue();
                    List s = new PriceCalculatorBuilder()
                            .setSortedPrices(priceVOS)
                            .setSamples(usageSamples)
                            .setUsageNormalizer(UsageNormalizer::normalizeVolumeUsage)
                            .build()
                            .calculate();

                    ret.addAll(s);
                }

                return map(e(BillingConstants.SPENDING_DATA_VOLUME, ret));
            }

            @Override
            protected List generateSpendingDetails(Map<String, List<UsageSample>> samples) {
                List<UsageSample> allSamples = samples.get(BillingConstants.SPENDING_DATA_VOLUME);
                Map<String, DataVolumeSpending> tmp = new HashMap<>();
                for (UsageSample s : allSamples) {
                    DataVolumeUsageSample ds = (DataVolumeUsageSample) s;
                    DataVolumeSpending spending = tmp.get(ds.getVolumeUuid());
                    if (spending == null) {
                        spending = new DataVolumeSpending();
                        spending.resourceName = ds.getVolumeName();
                        spending.resourceUuid = ds.getVolumeUuid();
                        spending.sizeInventory = new ArrayList<>();
                        spending.hypervisorType = ds.getHypervisorType();
                        tmp.put(ds.getVolumeUuid(), spending);
                    }

                    DataVolumeSpendingInventory inv = new DataVolumeSpendingInventory();
                    inv.startTime = s.getStartTime();
                    inv.endTime = s.getEndTime();
                    inv.spending = ds.getCost();
                    inv.volumeSize = (long) ds.getUsage();
                    spending.sizeInventory.add(inv);
                    spending.spending += ds.getCost();
                }

                return tmp.values().stream().collect(Collectors.toList());
            }
        }.generate();
    }
}
