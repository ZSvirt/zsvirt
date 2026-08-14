package org.zstack.billing;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.ObjectUtils;
import org.zstack.utils.TimeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by xing5 on 2016/6/7.
 */
public class PriceCalculatorBuilder {
    private static final CLogger logger = Utils.getLogger(PriceCalculatorBuilder.class);

    private List<PriceVO> sortedPrices;
    private List<? extends UsageSample> samples;
    private UsageNormalizer usageNormalizer;

    public PriceCalculatorBuilder setSortedPrices(List<PriceVO> prices) {
        this.sortedPrices = prices
                .stream()
                .sorted(Comparator.comparingLong(PriceVO::getDateInLong))
                .collect(Collectors.toList());
        if (this.sortedPrices.isEmpty()) {
            throw new CloudRuntimeException("cannot set empty prices");
        }
        return this;
    }

    public PriceCalculatorBuilder setSamples(List<? extends UsageSample> samples) {
        this.samples = samples;
        return this;
    }

    class RangePrice {
        UsageSample sample;
        PriceVO price;
        long startTime;
        long endTime;

        @Override
        public String toString() {
            return String.format("from %s (%s) to %s (%s), duration: %s seconds, price: %s",
                    new Date(startTime), startTime, new Date(endTime), endTime,
                    TimeUnit.MILLISECONDS.toSeconds(endTime - startTime),
                    price.getPrice());
        }
    }

    class RangePriceBuilder {
        private UsageSample sample;
        private PriceVO cache;

        public RangePriceBuilder(UsageSample sample) {
            this.sample = sample;
        }

        private UsageSample copySample() {
            return ObjectUtils.newAndCopy(sample, sample.getClass());
        }

        private void checkRange(RangePrice rp) {
            if (rp.endTime < rp.startTime) {
                throw new CloudRuntimeException(String.format("invalid range price %s", rp));
            }
        }

        List<RangePrice> push(PriceVO current, boolean isLast) {
            if (cache == null) {
                cache = current;

                if (current.getEndDateInLong() != null) {
                    List<RangePrice> subRangePrices = push(current, current.getDateInLong(), current.getEndDateInLong());
                    cache = null;
                    return subRangePrices;
                }

                if (!isLast) {
                    return null;
                }
            }

            PriceVO lowBound = cache;
            PriceVO highBound = current;
            cache = current;

            List<RangePrice> rangePrices = push(lowBound, lowBound.getDateInLong(), highBound.getDateInLong());

            if (current.getEndDateInLong() != null) {
                List<RangePrice> subRangePrices = push(current, current.getDateInLong(), current.getEndDateInLong());
                if (subRangePrices != null) {
                    if (rangePrices == null) {
                        rangePrices = subRangePrices;
                    } else {
                        rangePrices.addAll(subRangePrices);
                    }
                }

                if (isLast) {
                    return rangePrices;
                }
            }

            if (isLast) {
                List<RangePrice> subRangePrices = push(current, current.getDateInLong(), sample.getEndTime());
                if (subRangePrices != null) {
                    if (rangePrices == null) {
                        rangePrices = subRangePrices;
                    } else {
                        rangePrices.addAll(subRangePrices);
                    }
                }
            }
            return rangePrices;
        }

        List<RangePrice> push(PriceVO priceVO, long priceStart, long priceEnd) {
            if (priceStart == priceEnd) {
                return null;
            }

            long sampleStartTime = sample.startTime;
            long sampleEndTime = sample.endTime;

            // in below comments, PS means the start time of the price range, PE means the end time of the price range
            // SS means the start time of the sample, SE means the the end time of the sample
            if (sampleEndTime < priceStart) {
                // in the case of:
                //
                //          PS          PE
                // SS   SE
                //
                return null;
            } else if (sampleStartTime <= priceStart
                    && sampleEndTime >= priceStart
                    && sampleEndTime <= priceEnd) {
                // in the case of:
                //
                //          PS          PE
                // SS            SE
                RangePrice rp = new RangePrice();
                rp.sample = copySample();
                rp.startTime = priceStart;
                rp.endTime = sample.endTime;
                rp.price = priceVO;
                checkRange(rp);
                return list(rp);
            } else if (sampleStartTime <= priceStart && sampleEndTime >= priceEnd) {
                // in the case of
                //
                //          PS          PE
                //  SS                          SE
                RangePrice rp1 = new RangePrice();
                rp1.sample = copySample();
                rp1.startTime = priceStart;
                rp1.endTime = priceEnd;
                rp1.price = priceVO;
                checkRange(rp1);
                return list(rp1);
            } else if (sampleStartTime >= priceStart && sampleEndTime <= priceEnd) {
                // in the case of
                //          PS          PE
                //              SS  SE
                RangePrice rp = new RangePrice();
                rp.sample = copySample();
                rp.startTime = sample.startTime;
                rp.endTime = sample.endTime;
                rp.price = priceVO;
                checkRange(rp);
                return list(rp);
            } else if (sampleStartTime >= priceStart
                    && sampleStartTime <= priceEnd
                    && sampleEndTime >= priceEnd) {
                // in the case of
                //          PS          PE
                //              SS              SE
                RangePrice rp1 = new RangePrice();
                rp1.sample = copySample();
                rp1.startTime = sample.getStartTime();
                rp1.endTime = priceEnd;
                rp1.price = priceVO;
                checkRange(rp1);

                return list(rp1);
            } else if (sampleStartTime >= priceEnd) {
                // in the case of
                //          PS          PE
                //                          SS      SE
                return null;
            }

            throw new CloudRuntimeException(String.format("should no be here[PS: %s, PE: %s, SS: %s, SE: %s",
                    priceStart, priceEnd, sample.startTime, sample.endTime));
        }

        RangePrice buildTheOnlyOne(PriceVO one) {
            // in below comments, SS = sample start time, SS = sample end time, PT = price time
            long priceTime = one.getDateInLong();
            if (priceTime < sample.startTime) {
                // in the case of
                //      SS          SE
                //  PT
                RangePrice rp = new RangePrice();
                rp.startTime = sample.startTime;
                rp.endTime = sample.endTime;
                rp.price = one;
                rp.sample = copySample();
                checkRange(rp);
                return rp;
            } else if (priceTime >= sample.startTime && priceTime <= sample.endTime) {
                // in the case of
                //      SS          SE
                //            PT
                RangePrice rp = new RangePrice();
                rp.startTime = priceTime;
                rp.endTime = sample.endTime;
                rp.sample = copySample();
                rp.price = one;
                checkRange(rp);
                return rp;
            } else if (priceTime >= sample.endTime) {
                // in the case of
                //      SS      SE
                //                  PT
                return null;
            }

            throw new CloudRuntimeException(String.format("should no be here[PT: %s, SS: %s, SE: %s",
                    priceTime, sample.startTime, sample.endTime));
        }
    }

    private List<RangePrice> toRangePrice(UsageSample sample) {
        List<RangePrice> rangePrices = new ArrayList<>();

        RangePriceBuilder builder = new RangePriceBuilder(sample);
        for (PriceVO p : sortedPrices) {
            List ret;
            if (sortedPrices.indexOf(p) == (sortedPrices.size() - 1)) {
                ret = builder.push(p, true);
            } else {
                ret = builder.push(p, false);
            }
            if (ret != null) {
                rangePrices.addAll(ret);
            }
        }

        if (logger.isTraceEnabled()) {
            logger.trace(String.format("for the sample[%s] %s, the price ranges are %s",
                    sample.getClass().getSimpleName(),
                    JSONObjectUtil.toJsonString(sample),
                    rangePrices));
        }

        return rangePrices;
    }

    public UsageNormalizer getUsageNormalizer() {
        return usageNormalizer;
    }

    public PriceCalculatorBuilder setUsageNormalizer(UsageNormalizer usageNormalizer) {
        this.usageNormalizer = usageNormalizer;
        return this;
    }

    public PriceCalculator build() {
        DebugUtils.Assert(samples != null, "you need to call setSamples() before calling build()");
        DebugUtils.Assert(usageNormalizer != null, "you need to call setUsageNormalizer() before calling build()");
        DebugUtils.Assert(sortedPrices != null, "you need to call setPrices() before calling build()");

        return () -> {
            List<UsageSample> ret = new ArrayList<>();
            for (UsageSample s : samples) {
                List<RangePrice> rangePrices = toRangePrice(s);

                for (RangePrice rp : rangePrices) {
                    UsageSample sample = rp.sample;
                    sample.startTime = rp.startTime;
                    sample.endTime = rp.endTime;
                    sample.cost = rp.price.getPrice()
                            * (TimeUnit.MILLISECONDS.toSeconds(rp.endTime) - TimeUnit.MILLISECONDS.toSeconds(rp.startTime))
                            / TimeUtils.parseTimeToSeconds(rp.price.getTimeUnit())
                            * usageNormalizer.normalizeUsage(sample.getUsage(), rp.price.getResourceUnit());
                    /*
                    logger.debug(String.format("xxxxxxxxxxxxxxxxxx %f * %d * %f = %f",
                                    rp.price.getPrice(),
                                    TimeUnit.MILLISECONDS.toSeconds(rp.endTime - rp.startTime),
                                    usageNormalizer.normalizeUsage(sample.getUsage(),
                                    rp.price.getResourceUnit()), sample.cost));
                    */
                    
                    ret.add(sample);
                }
            }

            return ret;
        };
    }
}
