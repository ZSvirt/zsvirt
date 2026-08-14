package org.zstack.compute.cpuPinning;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.vm.MevocoVmSystemTags;
import org.zstack.core.db.SQL;
import org.zstack.header.Component;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.kvm.KVMStartVmExtensionPoint;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CpuPinningBasicFactory implements CpuPinningFactory, Component, KVMStartVmExtensionPoint {
    private static final CLogger logger = Utils.getLogger(CpuPinningBasicFactory.class);

    static class CpuPinningRuleTO {
        long vCpu;
        String pCpuSet;
        static CpuPinningRuleTO valueOf(CpuPinningRelation cpuPinning) {
            CpuPinningRuleTO result = new CpuPinningRuleTO();
            result.vCpu = cpuPinning.vCpu;
            result.pCpuSet = CpuRangeSet.valueOf(cpuPinning.pCpuSet).toString();
            return result;
        }
    }

    @Override
    public List<CpuPinningRelation> getRelationFromString(String relation) {
        Map<Long, CpuPinningRelation> result = new HashMap<>();
        String[] rs = StringUtils.deleteWhitespace(relation).split(CpuPinningConstant.RULES_SEPARATOR);
        for (String r : rs) {
            String[] pvCpus = r.split(CpuPinningConstant.V_P_CPU_SEPARATOR);
            if (pvCpus.length != 2) {
                throw new OperationFailureException(argerr("invalid cpu pinning ref[%s]. correct example is [1,3:3-6,^5]", r));
            }

            Set<Long> vCpus = CpuRangeSet.originValueOf(pvCpus[0]);
            Set<Long> pCpus = CpuRangeSet.originValueOf(pvCpus[1]);
            vCpus.forEach(vCpu -> result.computeIfAbsent(vCpu, k -> new CpuPinningRelation(k, new HashSet<>())).pCpuSet.addAll(pCpus));
        }

        return new ArrayList<>(result.values());
    }

    @Override
    public String convertRelationToString(Collection<CpuPinningRelation> relations) {
        return String.join(CpuPinningConstant.RULES_SEPARATOR, relations.stream()
                .map(it -> String.valueOf(it.vCpu) + CpuPinningConstant.V_P_CPU_SEPARATOR + CpuRangeSet.valueOf(it.pCpuSet).toString())
                .collect(Collectors.toList()));
    }


    @Override
    public boolean start() {
        MevocoVmSystemTags.VM_CPU_PINNING.installValidator((resourceUuid, resourceType, systemTag) -> {
            String cpuPinning = MevocoVmSystemTags.VM_CPU_PINNING.getTokenByTag(systemTag, MevocoVmSystemTags.VM_CPU_PINNING_TOKEN);
            List<CpuPinningRelation> rules = getRelationFromString(cpuPinning);
            Set<Long> pCpuSet = new HashSet<>();
            rules.forEach(it -> pCpuSet.addAll(it.pCpuSet));
            if (pCpuSet.size() < rules.size()) {
                // todo: check subset
                logger.warn(String.format("the size of vCpuSet is greater than pCpuSet. uuid[%s], pinning rule[%s]",
                        resourceUuid, cpuPinning));
            }

            Integer pCpuNum = SQL.New("select cap.cpuNum from HostCapacityVO cap, VmInstanceVO vm" +
                    " where vm.uuid =:uuid" +
                    " and cap.uuid = vm.hostUuid")
                    .param("uuid", resourceUuid).find();
            if (pCpuNum != null && pCpuSet.stream().anyMatch(it -> it >= pCpuNum)) {
                throw new OperationFailureException(argerr("the host vm located only have % CPUs", pCpuNum));
            }
        });

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        if (!MevocoVmSystemTags.VM_CPU_PINNING.hasTag(spec.getVmInventory().getUuid())) {
            return;
        }

        String vmCpuPinning = MevocoVmSystemTags.VM_CPU_PINNING.getTokenByResourceUuid(spec.getVmInventory().getUuid(), MevocoVmSystemTags.VM_CPU_PINNING_TOKEN);
        List<CpuPinningRuleTO> rules = getRelationFromString(vmCpuPinning).stream().map(CpuPinningRuleTO::valueOf).collect(Collectors.toList());
        rules.removeIf(it -> it.vCpu >= spec.getVmInventory().getCpuNum());
        if (rules.isEmpty()) {
            return;
        }

        cmd.getAddons().put("cpuPinning", rules);
    }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {

    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {

    }
}
