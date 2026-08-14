package org.zstack.billing.spendingcalculator.volume.root;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.billing.*;
import org.zstack.billing.spendingcalculator.SpendingCalculator;
import org.zstack.billing.userconfig.BillingUserConfigUtils;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.kvm.KVMConstant;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.*;

/**
 * Created by xing5 on 2016/6/15.
 */
public class RootVolumeSpendingCalculator implements SpendingCalculator {
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public Spending calculate(SpendingStruct param) {
        return new BillingTemplate(param) {
            @Override
            protected Class getUsageClass() {
                return RootVolumeUsageVO.class;
            }

            @Override
            protected String getUsageResourceUuidFiledName() {
                return "volumeUuid";
            }

            @Override
            protected List<String> getResourceNamesForPrice() {
                return list(BillingConstants.SPENDING_ROOT_VOLUME);
            }

            @Override
            protected String getSpendingType() {
                return BillingConstants.SPENDING_TYPE_ROOT_VOLUME;
            }

            @Override
            protected RangeOp returnRangeOp(List cache, Usage co) {
                RootVolumeUsageVO d = (RootVolumeUsageVO) co;
                if (VolumeStatus.Ready.toString().equals(d.getVolumeStatus())) {
                    if (cache.isEmpty()) {
                        return RangeOp.ADD_TO_CACHE;
                    }
                    RootVolumeUsageVO lastVO = ((List<RootVolumeUsageVO>)cache).get(cache.size() - 1);
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
                RootVolumeUsageVO r = (RootVolumeUsageVO) range.getUsageCO();
                SampleBundle volume = new SampleBundle();
                volume.resourceName = BillingConstants.SPENDING_ROOT_VOLUME;
                String hypervisorType;

                if (r.getInventory() == null) {
                    hypervisorType = KVMConstant.KVM_HYPERVISOR_TYPE;
                }else {
                    String format = JSONObjectUtil.toObject(r.getInventory(), VolumeInventory.class).getFormat();
                    hypervisorType = KVMConstant.KVM_HYPERVISOR_TYPE;
                }

                RootVolumeUsageSample rs = new RootVolumeUsageSample();
                rs.setVolumeName(r.getVolumeName());
                rs.setVolumeUuid(r.getVolumeUuid());
                rs.setUsage(r.getVolumeSize());
                RootVolumeUsageExtensionVO extensionVO = Q.New(RootVolumeUsageExtensionVO.class)
                        .eq(RootVolumeUsageExtensionVO_.id, r.getId())
                        .find();
                if (extensionVO != null ) {
                    rs.setResourcePriceUserConfig(extensionVO.getResourcePriceUserConfig());
                }
                rs.setHypervisorType(hypervisorType);
                volume.sample = rs;
                return list(volume);
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
                for (UsageSample sample: samples.get(BillingConstants.SPENDING_ROOT_VOLUME)) {
                    RootVolumeUsageSample usageSample = (RootVolumeUsageSample) sample;
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

                return map(e(BillingConstants.SPENDING_ROOT_VOLUME, ret));
            }

            @Override
            protected List generateSpendingDetails(Map<String, List<UsageSample>> samples) {
                List<UsageSample> allSamples = samples.get(BillingConstants.SPENDING_ROOT_VOLUME);
                Map<String, RootVolumeSpending> tmp = new HashMap<>();
                for (UsageSample s : allSamples) {
                    RootVolumeUsageSample ds = (RootVolumeUsageSample) s;
                    RootVolumeSpending spending = tmp.get(ds.getVolumeUuid());
                    if (spending == null) {
                        spending = new RootVolumeSpending();
                        spending.resourceName = ds.getVolumeName();
                        spending.resourceUuid = ds.getVolumeUuid();
                        spending.sizeInventory = new ArrayList<>();
                        spending.hypervisorType = ds.getHypervisorType();
                        tmp.put(ds.getVolumeUuid(), spending);
                    }

                    RootVolumeSpendingInventory inv = new RootVolumeSpendingInventory ();
                    inv.startTime = s.getStartTime();
                    inv.endTime = s.getEndTime();
                    inv.spending = ds.getCost();
                    inv.volumeSize = (long) ds.getUsage();
                    spending.sizeInventory.add(inv);
                    spending.spending += ds.getCost();
                }

                return new ArrayList<>(tmp.values());
            }
        }.generate();
    }
}
