package org.zstack.billing.spendingcalculator.snapshot;

import org.zstack.billing.*;
import org.zstack.billing.spendingcalculator.SpendingCalculator;
import org.zstack.header.storage.snapshot.VolumeSnapshotStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionDSL.*;

/**
 * Created by camile on 2017/5/16.
 */
public class SnapShotSpendingCalculator implements SpendingCalculator {
    @Override
    public Spending calculate(SpendingStruct param) {
        return new BillingTemplate(param) {
            @Override
            protected Class getUsageClass() {
                return SnapShotUsageVO.class;
            }

            @Override
            protected String getUsageResourceUuidFiledName() {
                return "SnapshotUuid";
            }

            @Override
            protected List<String> getResourceNamesForPrice() {
                return list(BillingConstants.SPENDING_SNAPSHOT);
            }

            @Override
            protected String getSpendingType() {
                return BillingConstants.SPENDING_TYPE_SNAPSHOT;
            }

            @Override
            protected RangeOp returnRangeOp(List cache, Usage co) {
                SnapShotUsageVO d = (SnapShotUsageVO) co;
                if (VolumeSnapshotStatus.Ready.toString().equals(d.getSnapshotStatus())) {
                    return RangeOp.ADD_TO_CACHE;
                } else if (!cache.isEmpty() && VolumeSnapshotStatus.Deleted.toString().equals(d.getSnapshotStatus())) {
                    return RangeOp.CLOSE;
                } else {
                    return RangeOp.SKIP;
                }
            }

            @Override
            protected List<SampleBundle> generateSampleBundle(ChargeRange range) {
                SnapShotUsageVO r = (SnapShotUsageVO) range.getUsageCO();
                SampleBundle bundle = new SampleBundle();
                bundle.resourceName = BillingConstants.SPENDING_SNAPSHOT;

                SnapshotUsageSample ss = new SnapshotUsageSample();
                ss.setAccountUuid(r.getAccountUuid());
                ss.setSnapshotName(r.getSnapshotName());
                ss.setSnapshotUuid(r.getSnapshotUuid());
                ss.setUsage(r.getSnapshotSize());
                bundle.sample = ss;
                return list(bundle);
            }

            @Override
            protected Map<String, List<UsageSample>> evaluateSampleSpending(Map<String, List<UsageSample>> samples, List<PriceVO> prices) {
                List s = samples.get(BillingConstants.SPENDING_SNAPSHOT);

                s = new PriceCalculatorBuilder().setSortedPrices(prices)
                        .setSamples(s).setUsageNormalizer(UsageNormalizer::normalizeVolumeUsage).build().calculate();
                return map(e(BillingConstants.SPENDING_SNAPSHOT, s));
            }

            @Override
            protected List generateSpendingDetails(Map<String, List<UsageSample>> samples) {
                List<UsageSample> allSamples = samples.get(BillingConstants.SPENDING_SNAPSHOT);
                Map<String, SnapshotSpending> tmp = new HashMap<>();
                for (UsageSample s : allSamples) {
                    SnapshotUsageSample ds = (SnapshotUsageSample) s;
                    SnapshotSpending spending = tmp.get(ds.getSnapshotUuid());
                    if (spending == null) {
                        spending = new SnapshotSpending();
                        spending.resourceName = ds.getSnapshotName();
                        spending.resourceUuid = ds.getSnapshotUuid();
                        spending.sizeInventory = new ArrayList<>();
                        tmp.put(ds.getSnapshotUuid(), spending);
                    }

                    SnapShotSpendingInventory inv = new SnapShotSpendingInventory();
                    inv.startTime = s.getStartTime();
                    inv.endTime = s.getEndTime();
                    inv.spending = ds.getCost();
                    inv.snapshotSize = (long) ds.getUsage();
                    spending.sizeInventory.add(inv);
                    spending.spending += ds.getCost();
                }

                return tmp.values().stream().collect(Collectors.toList());
            }
        }.generate();
    }
}
