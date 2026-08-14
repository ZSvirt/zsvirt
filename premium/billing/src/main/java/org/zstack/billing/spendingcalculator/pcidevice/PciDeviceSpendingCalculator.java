package org.zstack.billing.spendingcalculator.pcidevice;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.billing.*;
import org.zstack.billing.spendingcalculator.SpendingCalculator;
import org.zstack.core.db.Q;
import org.zstack.pciDevice.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by shixin.ruan on 2018/05/05.
 */
public class PciDeviceSpendingCalculator implements SpendingCalculator {
    @Autowired
    private PciDeviceManager pciDeviceManager;

    private static final CLogger logger = Utils.getLogger(PciDeviceSpendingCalculator.class);

    @Override
    public Spending calculate(SpendingStruct param) {
        List<PciDeviceUsageVO> pciDeviceUsageVOS = Q.New(PciDeviceUsageVO.class).eq(PciDeviceUsageVO_.accountUuid, param.getAccountUuid())
                .list();
        if (pciDeviceUsageVOS == null || pciDeviceUsageVOS.isEmpty()) {
            return null;
        }

        Map<String, String> pciDeviceOfferingMap = new HashMap<>();
        for (PciDeviceUsageVO pciDeviceUsageVO: pciDeviceUsageVOS) {
            if (pciDeviceOfferingMap.get(pciDeviceUsageVO.getPciDeviceUuid()) != null) {
                continue;
            }

            /* subvendorId and subdeviceId maybe null */
            List<PciDeviceOfferingVO> offeringVos;
            if (pciDeviceUsageVO.getSubvendorId() == null && pciDeviceUsageVO.getSubdeviceId() == null) {
                offeringVos = Q.New(PciDeviceOfferingVO.class).eq(PciDeviceOfferingVO_.vendorId, pciDeviceUsageVO.getVendorId())
                        .eq(PciDeviceOfferingVO_.deviceId, pciDeviceUsageVO.getDeviceId()).isNull(PciDeviceOfferingVO_.subvendorId)
                        .isNull(PciDeviceOfferingVO_.subdeviceId).list();
            } else if (pciDeviceUsageVO.getSubvendorId() != null && pciDeviceUsageVO.getSubdeviceId() == null) {
                offeringVos = Q.New(PciDeviceOfferingVO.class).eq(PciDeviceOfferingVO_.vendorId, pciDeviceUsageVO.getVendorId())
                        .eq(PciDeviceOfferingVO_.deviceId, pciDeviceUsageVO.getDeviceId()).eq(PciDeviceOfferingVO_.subvendorId, pciDeviceUsageVO.getSubvendorId())
                        .isNull(PciDeviceOfferingVO_.subdeviceId).list();
            } else if (pciDeviceUsageVO.getSubvendorId() == null && pciDeviceUsageVO.getSubdeviceId() != null) {
                offeringVos = Q.New(PciDeviceOfferingVO.class).eq(PciDeviceOfferingVO_.vendorId, pciDeviceUsageVO.getVendorId())
                        .eq(PciDeviceOfferingVO_.deviceId, pciDeviceUsageVO.getDeviceId()).isNull(PciDeviceOfferingVO_.subvendorId)
                        .eq(PciDeviceOfferingVO_.subdeviceId, pciDeviceUsageVO.getSubdeviceId()).list();
            } else {
                offeringVos = Q.New(PciDeviceOfferingVO.class).eq(PciDeviceOfferingVO_.vendorId, pciDeviceUsageVO.getVendorId())
                        .eq(PciDeviceOfferingVO_.deviceId, pciDeviceUsageVO.getDeviceId()).eq(PciDeviceOfferingVO_.subvendorId, pciDeviceUsageVO.getSubvendorId())
                        .eq(PciDeviceOfferingVO_.subdeviceId, pciDeviceUsageVO.getSubdeviceId()).list();
            }
            
            if (offeringVos == null || offeringVos.isEmpty()) {
                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("pciDevice [uuid: %s, vendorid %s, deviceid %s subVendorId %s, subDeviceId %s] did not have pciDeviceOffering",
                            pciDeviceUsageVO.getPciDeviceUuid(), pciDeviceUsageVO.getVendorId(), pciDeviceUsageVO.getDeviceId(), pciDeviceUsageVO.getSubvendorId(),
                            pciDeviceUsageVO.getSubdeviceId()));
                }
                continue;
            }

            if (offeringVos.size() > 1) {
                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("pciDevice [uuid: %s] has more than 1 pciDeviceOffering [%s]", pciDeviceUsageVO.getPciDeviceUuid(),
                            offeringVos.stream().map(PciDeviceOfferingVO::getUuid).collect(Collectors.toList())));
                }
            }
            pciDeviceOfferingMap.put(pciDeviceUsageVO.getPciDeviceUuid(), offeringVos.get(0).getUuid());
        }

        if (pciDeviceOfferingMap.size() < 1) {
            return null;
        }

        return new BillingTemplate(param) {
            @Override
            protected Class getUsageClass() {
                return PciDeviceUsageVO.class;
            }

            @Override
            protected String getUsageResourceUuidFiledName() {
                return "pciDeviceUuid";
            }

            @Override
            protected List<String> getResourceNamesForPrice() {
                return asList(BillingConstants.SPENDING_PCI_DEVICE);
            }

            @Override
            protected String getSpendingType() {
                return BillingConstants.SPENDING_TYPE_PCI_DEVICE;
            }

            @Override
            protected RangeOp returnRangeOp(List cache, Usage co) {
                PciDeviceUsageVO gpuCo = (PciDeviceUsageVO) co;
                /**                        cache is empty   |  cache is not empty
                 * status: Attached           ADD_TO_CACHE          ADD_TO_CACHE
                 * status: Not Attached       SKIP                  CLOSE
                 */
                if (PciDeviceStatus.Attached.toString().equals(gpuCo.getStatus())) {
                    return RangeOp.ADD_TO_CACHE;
                }

                if (cache.isEmpty() && !PciDeviceStatus.Attached.toString().equals(gpuCo.getStatus())) {
                    return RangeOp.SKIP;
                }

                return RangeOp.CLOSE;
            }

            @Override
            protected List<SampleBundle> generateSampleBundle(ChargeRange range) {
                PciDeviceUsageVO d = (PciDeviceUsageVO) range.getUsageCO();
                SampleBundle bundle = new SampleBundle();
                bundle.resourceName = BillingConstants.SPENDING_PCI_DEVICE;

                PciDeviceUsageSample s = new PciDeviceUsageSample();
                s.setAccountUuid(d.getAccountUuid());
                s.setPciDeviceUuid(d.getPciDeviceUuid());
                s.setDescription(d.getDescription());
                s.setVmName(d.getVmName());
                s.setUsage(1.0);
                bundle.sample = s;
                return list(bundle);
            }

            @Override
            protected Map<String, List<UsageSample>> evaluateSampleSpending(Map<String, List<UsageSample>> samples, List<PriceVO> prices) {
                /* group pricevo by pciDeviceOfferingUuid */
                Map<String, List<PriceVO>> priceMap = new HashMap<>();
                for (PriceVO co : prices) {
                    String offeringUuid = Q.New(PricePciDeviceOfferingRefVO.class).select(PricePciDeviceOfferingRefVO_.pciDeviceOfferingUuid)
                            .eq(PricePciDeviceOfferingRefVO_.priceUuid, co.getUuid()).findValue();
                    if (offeringUuid == null) {
                        logger.debug(String.format("can not find pciDeviceUuid for price [uuid:%s]", co.getUuid()));
                        continue;
                    }

                    List<PriceVO> gpuPriceList = priceMap.get(offeringUuid);
                    if (gpuPriceList == null) {
                        gpuPriceList = new ArrayList<>();
                        priceMap.put(offeringUuid, gpuPriceList);
                    }
                    gpuPriceList.add(co);
                }

                /* group sample by pciDeviceOfferingUuid */
                Map<String, List<UsageSample>> sampleMap = new HashMap<>();
                for (UsageSample sample: samples.get(BillingConstants.SPENDING_PCI_DEVICE)) {
                    PciDeviceUsageSample gpu = (PciDeviceUsageSample) sample;
                    String pciDeviceOfferingUuid = pciDeviceOfferingMap.get(gpu.getPciDeviceUuid());
                    if (pciDeviceOfferingUuid == null) {
                        continue;
                    }

                    List<UsageSample> gpuSampleList = sampleMap.get(pciDeviceOfferingUuid);
                    if (gpuSampleList == null) {
                        gpuSampleList = new ArrayList<>();
                        sampleMap.put(pciDeviceOfferingUuid, gpuSampleList);
                    }
                    gpuSampleList.add(sample);
                }

                /* calculate for each pciDevice Uuid */
                Map<String, List<UsageSample>> ret = new HashMap<>();
                for (Map.Entry<String, List<PriceVO>> e: priceMap.entrySet()) {
                    String pciDeviceOfferingUuid = e.getKey();
                    List<UsageSample> offeringSamples = sampleMap.get(pciDeviceOfferingUuid);
                    if (offeringSamples == null) {
                        continue;
                    }

                    List<PriceVO> offeringPrices = e.getValue();
                    List calculatedSamples = new PriceCalculatorBuilder()
                            .setSortedPrices(offeringPrices)
                            .setSamples(offeringSamples)
                            .setUsageNormalizer(UsageNormalizer::normalizeGpuUsage)
                            .build()
                            .calculate();

                    ret.put(pciDeviceOfferingUuid, calculatedSamples);
                }

                return ret;
            }

            @Override
            protected List generateSpendingDetails(Map<String, List<UsageSample>> samples) {
                Map<String, PciDeviceSpending> tmp = new HashMap<>();
                for (Map.Entry<String, List<UsageSample>> e: samples.entrySet()) {
                    for (UsageSample s : e.getValue()) {
                        PciDeviceUsageSample gs = (PciDeviceUsageSample) s;
                        PciDeviceSpending spending = tmp.get(gs.getPciDeviceUuid());
                        if (spending == null) {
                            spending = new PciDeviceSpending();
                            spending.resourceName = gs.getDescription();
                            spending.resourceUuid = gs.getPciDeviceUuid();
                            spending.sizeInventory = new ArrayList<>();
                            tmp.put(gs.getPciDeviceUuid(), spending);
                        }

                        PciDeviceSpendingInventory inv = new PciDeviceSpendingInventory();
                        inv.startTime = s.getStartTime();
                        inv.endTime = s.getEndTime();
                        inv.spending = gs.getCost();
                        inv.vmName = gs.getVmName();
                        spending.sizeInventory.add(inv);
                        spending.spending += gs.getCost();
                    }
                }

                return tmp.values().stream().collect(Collectors.toList());
            }
        }.generate();
    }
}
