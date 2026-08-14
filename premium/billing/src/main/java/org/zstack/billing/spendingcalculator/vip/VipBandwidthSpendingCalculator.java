package org.zstack.billing.spendingcalculator.vip;

import org.zstack.billing.*;
import org.zstack.billing.spendingcalculator.SpendingCalculator;
import org.zstack.network.service.vip.VipCanonicalEvents;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by lining on 2018/11/20.
 */
public class VipBandwidthSpendingCalculator implements SpendingCalculator {
    @Override
    public Spending calculate(SpendingStruct param) {
        return new BillingTemplate(param) {
            @Override
            protected Class getUsageClass() {
                return PubIpVipBandwidthUsageVO.class;
            }

            @Override
            protected String getUsageResourceUuidFiledName() {
                return "vipUuid";
            }

            @Override
            protected List<String> getResourceNamesForPrice() {
                return list(BillingConstants.SPENDING_VIP_BANDWIDTH_IN, BillingConstants.SPENDING_VIP_BANDWIDTH_OUT);
            }

            @Override
            protected String getSpendingType() {
                return BillingConstants.SPENDING_PUBLIC_IP_VIP_BANDWIDTH;
            }

            @Override
            protected RangeOp returnRangeOp(List cache, Usage co) {
                PubIpVipBandwidthUsageVO d = (PubIpVipBandwidthUsageVO) co;

                if (VipCanonicalEvents.VIP_STATUS_CREATED.equals(d.getVipStatus())) {
                    if (cache.isEmpty()) {
                        return RangeOp.ADD_TO_CACHE;
                    }

                    List<PubIpVipBandwidthUsageVO> usageVOS = cache;
                    for (PubIpVipBandwidthUsageVO usageVO : usageVOS) {
                        if (usageVO.getBandwidthIn().equals(d.getBandwidthIn()) && usageVO.getBandwidthOut().equals(d.getBandwidthOut())) {
                            return RangeOp.ADD_TO_CACHE;
                        }
                    }

                    return RangeOp.CLOSE_AND_ADD_TO_CACHE;
                } else if (VipCanonicalEvents.VIP_STATUS_DELETED.equals(d.getVipStatus()) && cache.isEmpty()) {
                    return RangeOp.SKIP;
                } else {
                    return RangeOp.CLOSE;
                }
            }

            @Override
            protected List<SampleBundle> generateSampleBundle(ChargeRange range) {
                List<SampleBundle> bundles = new ArrayList<>();
                PubIpVipBandwidthUsageVO r = (PubIpVipBandwidthUsageVO) range.getUsageCO();

                SampleBundle bandwidthIn = new SampleBundle();
                bandwidthIn.resourceName = BillingConstants.SPENDING_VIP_BANDWIDTH_IN;
                VipBandwidthInUsageSample inUsageSample = new VipBandwidthInUsageSample();
                inUsageSample.setVipUuid(r.getVipUuid());
                inUsageSample.setUsage(r.getBandwidthIn());
                inUsageSample.setVipName(r.getVipName());
                inUsageSample.setVipIp(r.getVipIp());
                bandwidthIn.sample = inUsageSample;
                bundles.add(bandwidthIn);

                SampleBundle bandwidthOut = new SampleBundle();
                bandwidthOut.resourceName = BillingConstants.SPENDING_VIP_BANDWIDTH_OUT;
                VipBandwidthOutUsageSample outUsageSample = new VipBandwidthOutUsageSample();
                outUsageSample.setVipUuid(r.getVipUuid());
                outUsageSample.setUsage(r.getBandwidthOut());
                outUsageSample.setVipName(r.getVipName());
                outUsageSample.setVipIp(r.getVipIp());
                bandwidthOut.sample = outUsageSample;
                bundles.add(bandwidthOut);

                return bundles;
            }

            @Override
            protected Map<String, List<UsageSample>> evaluateSampleSpending(Map<String, List<UsageSample>> samples, List<PriceVO> prices) {
                List<PriceVO> inPriceVO = new ArrayList<>();
                List<PriceVO> outPriceVO = new ArrayList<>();

                for (PriceVO co : prices) {
                    if (BillingConstants.SPENDING_VIP_BANDWIDTH_IN.equals(co.getResourceName())) {
                        inPriceVO.add(co);
                    } else if (BillingConstants.SPENDING_VIP_BANDWIDTH_OUT.equals(co.getResourceName())) {
                        outPriceVO.add(co);
                    }
                }

                Map<String, List<UsageSample>> ret = new HashMap<>();
                if (!inPriceVO.isEmpty()) {
                    List inSamples = samples.get(BillingConstants.SPENDING_VIP_BANDWIDTH_IN);
                    if (inSamples != null) {
                        inSamples = new PriceCalculatorBuilder()
                                .setSortedPrices(inPriceVO)
                                .setSamples(inSamples)
                                .setUsageNormalizer(UsageNormalizer::normalizeBandwidthUsage)
                                .build()
                                .calculate();
                        ret.put(BillingConstants.SPENDING_VIP_BANDWIDTH_IN, inSamples);
                    }
                }

                if (!outPriceVO.isEmpty()) {
                    List outSamples = samples.get(BillingConstants.SPENDING_VIP_BANDWIDTH_OUT);
                    if (outSamples != null) {
                        outSamples = new PriceCalculatorBuilder()
                                .setSortedPrices(outPriceVO)
                                .setSamples(outSamples)
                                .setUsageNormalizer(UsageNormalizer::normalizeBandwidthUsage)
                                .build()
                                .calculate();
                        ret.put(BillingConstants.SPENDING_VIP_BANDWIDTH_OUT, outSamples);
                    }
                }

                return ret;
            }

            @Override
            protected List generateSpendingDetails(Map<String, List<UsageSample>> samples) {
                List inSamples = samples.get(BillingConstants.SPENDING_VIP_BANDWIDTH_IN);
                List outSamples = samples.get(BillingConstants.SPENDING_VIP_BANDWIDTH_OUT);

                Map<String, PubIpVipBandwidthSpending> tmp = new HashMap<>();
                if (inSamples != null) {
                    inSamples.forEach(s -> {
                        VipBandwidthInUsageSample cs = (VipBandwidthInUsageSample) s;
                        PubIpVipBandwidthSpending spending = tmp.get(cs.getVipUuid());
                        if (spending == null) {
                            spending = new PubIpVipBandwidthSpending();
                            spending.resourceName = cs.getVipName();
                            spending.resourceUuid = cs.getVipUuid();
                            spending.vipIp = cs.getVipIp();
                            tmp.put(cs.getVipUuid(), spending);
                        }

                        if (spending.bandwidthInInventory == null) {
                            spending.bandwidthInInventory = new ArrayList<>();
                        }

                        VipBandwidthSpendingDetails sd = new VipBandwidthSpendingDetails();
                        sd.spending = cs.getCost();
                        sd.startTime = cs.getStartTime();
                        sd.endTime = cs.getEndTime();
                        sd.bandwidthSize = (long) cs.getUsage();
                        spending.bandwidthInInventory.add(sd);
                    });
                }

                if (outSamples != null) {
                    outSamples.forEach(s -> {
                        VipBandwidthOutUsageSample cs = (VipBandwidthOutUsageSample) s;
                        PubIpVipBandwidthSpending spending = tmp.get(cs.getVipUuid());
                        if (spending == null) {
                            spending = new PubIpVipBandwidthSpending();
                            spending.resourceName = cs.getVipName();
                            spending.resourceUuid = cs.getVipUuid();
                            spending.vipIp = cs.getVipIp();
                            tmp.put(cs.getVipUuid(), spending);
                        }

                        if (spending.bandwidthOutInventory == null) {
                            spending.bandwidthOutInventory = new ArrayList<>();
                        }

                        VipBandwidthSpendingDetails sd = new VipBandwidthSpendingDetails();
                        sd.spending = cs.getCost();
                        sd.startTime = cs.getStartTime();
                        sd.endTime = cs.getEndTime();
                        sd.bandwidthSize = (long) cs.getUsage();
                        spending.bandwidthOutInventory.add(sd);
                    });
                }

                List<PubIpVipBandwidthSpending> ret = tmp.values().stream().collect(Collectors.toList());
                ret.forEach(s -> {
                    if (s.bandwidthInInventory != null) {
                        s.spending += s.bandwidthInInventory.stream().mapToDouble(i -> i.spending).sum();
                    }
                    if (s.bandwidthOutInventory != null) {
                        s.spending += s.bandwidthOutInventory.stream().mapToDouble(i -> i.spending).sum();
                    }
                });
                return ret;
            }
        }.generate();
    }
}
