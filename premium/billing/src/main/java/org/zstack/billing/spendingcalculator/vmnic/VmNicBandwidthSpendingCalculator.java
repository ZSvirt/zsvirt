package org.zstack.billing.spendingcalculator.vmnic;

import org.apache.commons.lang.StringUtils;
import org.zstack.billing.*;
import org.zstack.billing.spendingcalculator.SpendingCalculator;
import org.zstack.header.vm.VmInstanceState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by lining on 2018/11/20.
 */
public class VmNicBandwidthSpendingCalculator implements SpendingCalculator {
    @Override
    public Spending calculate(SpendingStruct param) {
        return new BillingTemplate(param) {
            @Override
            protected Class getUsageClass() {
                return PubIpVmNicBandwidthUsageVO.class;
            }

            @Override
            protected String getUsageResourceUuidFiledName() {
                return "vmNicUuid";
            }

            @Override
            protected List<String> getResourceNamesForPrice() {
                return list(BillingConstants.SPENDING_VM_NIC_BANDWIDTH_IN, BillingConstants.SPENDING_VM_NIC_BANDWIDTH_OUT);
            }

            @Override
            protected String getSpendingType() {
                return BillingConstants.SPENDING_PUBLIC_IP_VM_NIC_BANDWIDTH;
            }

            @Override
            protected RangeOp returnRangeOp(List cache, Usage co) {
                PubIpVmNicBandwidthUsageVO d = (PubIpVmNicBandwidthUsageVO) co;

                if ((VmInstanceState.Running.toString().equals(d.getVmNicStatus())
                        || VmInstanceState.Stopped.toString().equals(d.getVmNicStatus())
                        && StringUtils.isNotEmpty(d.getVmNicIp()))) {
                    if (cache.isEmpty()) {
                        return RangeOp.ADD_TO_CACHE;
                    }

                    List<PubIpVmNicBandwidthUsageVO> usageVOS = cache;
                    for (PubIpVmNicBandwidthUsageVO usageVO : usageVOS) {
                        if (usageVO.getBandwidthIn().equals(d.getBandwidthIn()) && usageVO.getBandwidthOut().equals(d.getBandwidthOut())) {
                            return RangeOp.ADD_TO_CACHE;
                        }
                    }

                    return RangeOp.CLOSE_AND_ADD_TO_CACHE;
                } else if (VmInstanceState.Destroyed.toString().equals(d.getVmNicStatus()) && cache.isEmpty()) {
                    return RangeOp.SKIP;
                } else {
                    return RangeOp.CLOSE;
                }
            }

            @Override
            protected List<SampleBundle> generateSampleBundle(ChargeRange range) {
                List<SampleBundle> bundles = new ArrayList<>();
                PubIpVmNicBandwidthUsageVO r = (PubIpVmNicBandwidthUsageVO) range.getUsageCO();

                SampleBundle bandwidthIn = new SampleBundle();
                bandwidthIn.resourceName = BillingConstants.SPENDING_VM_NIC_BANDWIDTH_IN;
                VmNicBandwidthInUsageSample inUsageSample = new VmNicBandwidthInUsageSample();
                inUsageSample.setVmNicUuid(r.getVmNicUuid());
                inUsageSample.setVmNicIp(r.getVmNicIp());
                inUsageSample.setUsage(r.getBandwidthIn());
                bandwidthIn.sample = inUsageSample;
                bundles.add(bandwidthIn);

                SampleBundle bandwidthOut = new SampleBundle();
                bandwidthOut.resourceName = BillingConstants.SPENDING_VM_NIC_BANDWIDTH_OUT;
                VmNicBandwidthOutUsageSample outUsageSample = new VmNicBandwidthOutUsageSample();
                outUsageSample.setVmNicUuid(r.getVmNicUuid());
                outUsageSample.setVmNicIp(r.getVmNicIp());
                outUsageSample.setUsage(r.getBandwidthOut());
                bandwidthOut.sample = outUsageSample;
                bundles.add(bandwidthOut);

                return bundles;
            }

            @Override
            protected Map<String, List<UsageSample>> evaluateSampleSpending(Map<String, List<UsageSample>> samples, List<PriceVO> prices) {
                List<PriceVO> inPriceVO = new ArrayList<>();
                List<PriceVO> outPriceVO = new ArrayList<>();

                for (PriceVO co : prices) {
                    if (BillingConstants.SPENDING_VM_NIC_BANDWIDTH_IN.equals(co.getResourceName())) {
                        inPriceVO.add(co);
                    } else if (BillingConstants.SPENDING_VM_NIC_BANDWIDTH_OUT.equals(co.getResourceName())) {
                        outPriceVO.add(co);
                    }
                }

                Map<String, List<UsageSample>> ret = new HashMap<>();
                if (!inPriceVO.isEmpty()) {
                    List inSamples = samples.get(BillingConstants.SPENDING_VM_NIC_BANDWIDTH_IN);
                    if (inSamples != null) {
                        inSamples = new PriceCalculatorBuilder()
                                .setSortedPrices(inPriceVO)
                                .setSamples(inSamples)
                                .setUsageNormalizer(UsageNormalizer::normalizeBandwidthUsage)
                                .build()
                                .calculate();
                        ret.put(BillingConstants.SPENDING_VM_NIC_BANDWIDTH_IN, inSamples);
                    }
                }

                if (!outPriceVO.isEmpty()) {
                    List outSamples = samples.get(BillingConstants.SPENDING_VM_NIC_BANDWIDTH_OUT);
                    if (outSamples != null) {
                        outSamples = new PriceCalculatorBuilder()
                                .setSortedPrices(outPriceVO)
                                .setSamples(outSamples)
                                .setUsageNormalizer(UsageNormalizer::normalizeBandwidthUsage)
                                .build()
                                .calculate();
                        ret.put(BillingConstants.SPENDING_VM_NIC_BANDWIDTH_OUT, outSamples);
                    }
                }

                return ret;
            }

            @Override
            protected List generateSpendingDetails(Map<String, List<UsageSample>> samples) {
                List inSamples = samples.get(BillingConstants.SPENDING_VM_NIC_BANDWIDTH_IN);
                List outSamples = samples.get(BillingConstants.SPENDING_VM_NIC_BANDWIDTH_OUT);

                Map<String, PubIpVmNicBandwidthSpending> tmp = new HashMap<>();
                if (inSamples != null) {
                    inSamples.forEach(s -> {
                        VmNicBandwidthInUsageSample cs = (VmNicBandwidthInUsageSample) s;
                        PubIpVmNicBandwidthSpending spending = tmp.get(cs.getVmNicUuid());
                        if (spending == null) {
                            spending = new PubIpVmNicBandwidthSpending();
                            spending.resourceName = null;
                            spending.resourceUuid = cs.getVmNicUuid();
                            spending.vmNicIp = cs.getVmNicIp();
                            tmp.put(cs.getVmNicUuid(), spending);
                        }

                        if (spending.bandwidthInInventory == null) {
                            spending.bandwidthInInventory = new ArrayList<>();
                        }

                        VmNicBandwidthSpendingDetails sd = new VmNicBandwidthSpendingDetails();
                        sd.spending = cs.getCost();
                        sd.startTime = cs.getStartTime();
                        sd.endTime = cs.getEndTime();
                        sd.bandwidthSize = (long)cs.getUsage();
                        spending.bandwidthInInventory.add(sd);
                    });
                }

                if (outSamples != null) {
                    outSamples.forEach(s -> {
                        VmNicBandwidthOutUsageSample cs = (VmNicBandwidthOutUsageSample) s;
                        PubIpVmNicBandwidthSpending spending = tmp.get(cs.getVmNicUuid());
                        if (spending == null) {
                            spending = new PubIpVmNicBandwidthSpending();
                            spending.resourceName = null;
                            spending.resourceUuid = cs.getVmNicUuid();
                            spending.vmNicIp = cs.getVmNicIp();
                            tmp.put(cs.getVmNicUuid(), spending);
                        }

                        if (spending.bandwidthOutInventory == null) {
                            spending.bandwidthOutInventory = new ArrayList<>();
                        }

                        VmNicBandwidthSpendingDetails sd = new VmNicBandwidthSpendingDetails();
                        sd.spending = cs.getCost();
                        sd.startTime = cs.getStartTime();
                        sd.endTime = cs.getEndTime();
                        sd.bandwidthSize = (long)cs.getUsage();
                        spending.bandwidthOutInventory.add(sd);
                    });
                }

                List<PubIpVmNicBandwidthSpending> ret = new ArrayList<>(tmp.values());
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
