package org.zstack.billing.spendingcalculator.vm;

import org.zstack.billing.*;
import org.zstack.billing.spendingcalculator.SpendingCalculator;
import org.zstack.core.db.Q;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.kvm.KVMConstant;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

/**
 * Created by frank on 3/4/2016.
 */
public class VmSpendingCalculator implements SpendingCalculator {
    private static final CLogger logger = Utils.getLogger(VmSpendingCalculator.class);

    public static String getHypervisorType(VmUsageVO vm) {
        if (Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm.getVmUuid()).isExists()){
            return Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vm.getVmUuid()).select(VmInstanceVO_.hypervisorType).findValue();
        } else {
            return KVMConstant.KVM_HYPERVISOR_TYPE;
        }
    }

    @Override
    public Spending calculate(final SpendingStruct param) {
        return new BillingTemplate(param) {
            @Override
            protected Class getUsageClass() {
                return VmUsageVO.class;
            }

            @Override
            protected String getUsageResourceUuidFiledName() {
                return "vmUuid";
            }

            @Override
            protected List<String> getResourceNamesForPrice() {
                return asList(BillingConstants.SPENDING_CPU, BillingConstants.SPENDING_MEMORY);
            }

            @Override
            protected String getSpendingType() {
                return BillingConstants.SPENDING_TYPE_VM;
            }

            private String transformVmInstanceState(String vmState) {
                // The suspended vm will generate billing
                if (VmInstanceState.Paused.toString().equals(vmState)) {
                    return VmInstanceState.Running.toString();
                }

                // Vm in unknown state does not generate billing
                if (VmInstanceState.Unknown.toString().equals(vmState)) {
                    return VmInstanceState.Stopped.toString();
                }

                return vmState;
            }

            @Override
            protected RangeOp returnRangeOp(List cache, Usage co) {
                VmUsageVO vmCO = (VmUsageVO) co;

                String state = transformVmInstanceState(vmCO.getState());
                if (VmInstanceState.Running.toString().equals(state)) {
                    if (cache.isEmpty()) {
                        return RangeOp.ADD_TO_CACHE;
                    }

                    List<VmUsageVO> usageVOS = cache;
                    VmUsageVO lastVO = usageVOS.get(usageVOS.size() - 1);
                    if (lastVO.getCpuNum() == vmCO.getCpuNum() && lastVO.getMemorySize() == vmCO.getMemorySize()) {
                        return RangeOp.ADD_TO_CACHE;
                    } else {
                        return RangeOp.CLOSE_AND_ADD_TO_CACHE;
                    }
                }

                if (VmInstanceState.Stopped.toString().equals(state) && cache.isEmpty()) {
                    return RangeOp.SKIP;
                } else if (VmInstanceState.Destroyed.toString().equals(state) && cache.isEmpty()) {
                    return RangeOp.SKIP;
                } else {
                    // the VM has stopped and there are previous records of Running
                    return RangeOp.CLOSE;
                }
            }

            @Override
            protected List<SampleBundle> generateSampleBundle(ChargeRange range) {
                List<SampleBundle> bundles = new ArrayList<>();
                VmUsageVO vm = (VmUsageVO) range.getUsageCO();

                String hypervisorType = getHypervisorType(vm);

                SampleBundle cpu = new SampleBundle();
                cpu.resourceName = BillingConstants.SPENDING_CPU;
                CpuUsageSample cs = new CpuUsageSample();
                cs.setVmName(vm.getName());
                cs.setVmUuid(vm.getVmUuid());
                cs.setUsage(vm.getCpuNum());
                cs.setHypervisorType(hypervisorType);
                cs.setCpuNum(vm.getCpuNum());
                cpu.sample = cs;
                bundles.add(cpu);

                SampleBundle mem = new SampleBundle();
                mem.resourceName = BillingConstants.SPENDING_MEMORY;
                MemoryUsageSample ms = new MemoryUsageSample();
                ms.setVmName(vm.getName());
                ms.setVmUuid(vm.getVmUuid());
                ms.setUsage(vm.getMemorySize());
                ms.setHypervisorType(hypervisorType);
                ms.setMemorySize(vm.getMemorySize());
                mem.sample = ms;
                bundles.add(mem);

                return bundles;
            }

            @Override
            protected Map<String, List<UsageSample>> evaluateSampleSpending(Map<String, List<UsageSample>> samples, List<PriceVO> prices) {
                List<PriceVO> cpuPriceVO = new ArrayList<>();
                List<PriceVO> memPriceVO = new ArrayList<>();
                for (PriceVO co : prices) {
                    if (BillingConstants.SPENDING_CPU.equals(co.getResourceName())) {
                        cpuPriceVO.add(co);
                    } else if (BillingConstants.SPENDING_MEMORY.equals(co.getResourceName())) {
                        memPriceVO.add(co);
                    }
                }

                Map<String, List<UsageSample>> ret = new HashMap<>();
                if (!cpuPriceVO.isEmpty()) {
                    List cpuSamples = samples.get(BillingConstants.SPENDING_CPU);
                    if (cpuSamples != null) {
                        cpuSamples = new PriceCalculatorBuilder()
                                .setSortedPrices(cpuPriceVO)
                                .setSamples(cpuSamples)
                                .setUsageNormalizer(UsageNormalizer::normalizeCpuUsage)
                                .build()
                                .calculate();
                        ret.put(BillingConstants.SPENDING_CPU, cpuSamples);
                    }
                }

                if (!memPriceVO.isEmpty()) {
                    List memSamples = samples.get(BillingConstants.SPENDING_MEMORY);
                    if (memSamples != null) {
                        memSamples = new PriceCalculatorBuilder()
                                .setSortedPrices(memPriceVO)
                                .setSamples(memSamples)
                                .setUsageNormalizer(UsageNormalizer::normalizeMemoryUsage)
                                .build()
                                .calculate();
                        ret.put(BillingConstants.SPENDING_MEMORY, memSamples);
                    }
                }

                return ret;
            }

            @Override
            protected List generateSpendingDetails(Map<String, List<UsageSample>> samples) {
                List cpuSamples = samples.get(BillingConstants.SPENDING_CPU);
                List memSamples = samples.get(BillingConstants.SPENDING_MEMORY);

                Map<String, VmSpending> tmp = new HashMap<>();
                if (cpuSamples != null) {
                    cpuSamples.forEach(s -> {
                        CpuUsageSample cs = (CpuUsageSample) s;
                        VmSpending spending = tmp.get(cs.getVmUuid());
                        if (spending == null) {
                            spending = new VmSpending();
                            spending.resourceName = cs.getVmName();
                            spending.resourceUuid = cs.getVmUuid();
                            spending.hypervisorType = cs.getHypervisorType();
                            tmp.put(cs.getVmUuid(), spending);
                        }

                        if (spending.cpuInventory == null) {
                            spending.cpuInventory = new ArrayList<>();
                        }

                        VmCPUSpendingDetails sd = new VmCPUSpendingDetails();
                        sd.spending = cs.getCost();
                        sd.startTime = cs.getStartTime();
                        sd.endTime = cs.getEndTime();
                        sd.cpuNum = cs.cpuNum;
                        spending.cpuInventory.add(sd);
                    });
                }

                if (memSamples != null) {
                    memSamples.forEach(s -> {
                        MemoryUsageSample cs = (MemoryUsageSample) s;
                        VmSpending spending = tmp.get(cs.getVmUuid());
                        if (spending == null) {
                            spending = new VmSpending();
                            spending.resourceName = cs.getVmName();
                            spending.resourceUuid = cs.getVmUuid();
                            spending.hypervisorType = cs.getHypervisorType();
                            tmp.put(cs.getVmUuid(), spending);
                        }

                        if (spending.memoryInventory == null) {
                            spending.memoryInventory = new ArrayList<>();
                        }

                        VmMemorySpendingDetails sd = new VmMemorySpendingDetails();
                        sd.spending = cs.getCost();
                        sd.startTime = cs.getStartTime();
                        sd.endTime = cs.getEndTime();
                        sd.memorySize = cs.memorySize;
                        spending.memoryInventory.add(sd);
                    });
                }

                List<VmSpending> ret = new ArrayList<>(tmp.values());
                ret.forEach(s -> {
                    if (s.cpuInventory != null) {
                        s.spending += s.cpuInventory.stream().mapToDouble(i -> i.spending).sum();
                    }
                    if (s.memoryInventory != null) {
                        s.spending += s.memoryInventory.stream().mapToDouble(i -> i.spending).sum();
                    }
                });
                return ret;
            }
        }.generate();
    }
}
