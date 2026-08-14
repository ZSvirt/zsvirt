package org.zstack.compute.vm.numa;

import org.apache.commons.lang.StringUtils;
import org.zstack.compute.vm.MevocoVmSystemTags;
import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.resourceconfig.ResourceConfigVO;
import org.zstack.resourceconfig.ResourceConfigVO_;

import java.util.*;

import static org.zstack.core.Platform.operr;


/**
 * Created by longtao.wu@zstack.io on 21/12/09
 */
public class VmNumaCpuPinInventory {

    private Map<Integer, Set<Integer>> hostCpuListByVmNumaCpuMap;

    public void setHostCpuListByVmNumaCpuMap(String vmUuid) {
        String relation = MevocoVmSystemTags.VM_CPU_PINNING.getTokenByResourceUuid(vmUuid, MevocoVmSystemTags.VM_CPU_PINNING_TOKEN);
        Map<Integer, Set<Integer>> result = new HashMap<>();
        String[] rs = StringUtils.deleteWhitespace(relation).split(VmNumaConstant.RULES_SEPARATOR);
        for (String r : rs) {
            String[] hostVmCpus = r.split(VmNumaConstant.V_P_CPU_SEPARATOR);
            Set<Integer> vmCpusSet = VmNumaUtils.originValueOf(hostVmCpus[0]);
            Set<Integer> hostCpuSet = VmNumaUtils.originValueOf(hostVmCpus[1]);
            vmCpusSet.forEach(vCpu -> result.computeIfAbsent(vCpu, k -> new HashSet<>()).addAll(hostCpuSet));
        }
        this.hostCpuListByVmNumaCpuMap = result;
    }

    public Map<Integer, Set<Integer>> getHostCpuListByVmNumaCpuMap() {
        return hostCpuListByVmNumaCpuMap;
    }

    public void checkBindingConditions(String vmUuid, Integer vmCpuSum, Map<Integer, HostNumaNodeInventory> hostNuma) {
        List<String> errors = new ArrayList<>();
        if (Q.New(ResourceConfigVO.class).eq(ResourceConfigVO_.name, "numa").eq(ResourceConfigVO_.resourceUuid, vmUuid).isExists()) {
            String hotPluginState = Q.New(ResourceConfigVO.class).select(ResourceConfigVO_.value).eq(ResourceConfigVO_.name, "numa").eq(ResourceConfigVO_.resourceUuid, vmUuid).findValue();
            if (hotPluginState.equals("true")) {
                errors.add(Platform.i18n("hot plug not close"));
            }
        }
        if (hostCpuListByVmNumaCpuMap.size() != vmCpuSum) {
            errors.add(Platform.i18n("vm cpu not all pinning"));
        }
        for (Map.Entry<Integer, Set<Integer>> entry : hostCpuListByVmNumaCpuMap.entrySet()) {
            if (!hostNuma.get(entry.getValue().iterator().next()).checkCpuList(entry.getValue())) {
                errors.add(Platform.i18n("cpu[%s] not pin in a same host numa node", entry.getKey().toString()));
            }
        }
        if (!errors.isEmpty()) {
            throw new OperationFailureException(operr(Platform.i18n("vm[%s] start fail,because numa is enable but: %s", vmUuid, String.join(VmNumaConstant.RULES_SEPARATOR, errors))));
        }
    }


}
