package org.zstack.compute.vm.numa;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.db.Q;
import org.zstack.header.host.HostNumaNodeVO;
import org.zstack.header.host.HostNumaNodeVO_;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Created by longtao.wu@zstack.io on 21/12/09
 */
public interface VmNumaFactory {

    List<VmNumaNodeInventory> generator();

    VmNumaConstant.ClusterType getType();

    default Map<Integer, HostNumaNodeInventory> getHostNumaNodesFromHostNumaNodeVO(String hostUuid) {
        List<HostNumaNodeVO> hostNumaNodeVOs = Q.New(HostNumaNodeVO.class).eq(HostNumaNodeVO_.hostUuid,
                hostUuid).list();
        Map<Integer, HostNumaNodeInventory> temHostNumaNodes = new HashMap<>();
        for (HostNumaNodeVO hostNode : hostNumaNodeVOs) {
            List<Integer> nodeCpuList = Arrays.stream(StringUtils.deleteWhitespace(hostNode.getNodeCPUs()).split(VmNumaConstant.CPU_SET_SEPARATOR)).map(Integer::valueOf).collect(Collectors.toList());
            HostNumaNodeInventory value = new HostNumaNodeInventory(hostNode);
            for (Integer nodeCpu : nodeCpuList) {
                temHostNumaNodes.put(nodeCpu, value);
            }
        }
        return temHostNumaNodes;
    }
}
