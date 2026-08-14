package org.zstack.compute.vm.numa;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.host.HostNumaNodeVO;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Created by longtao.wu@zstack.io on 21/12/09
 */
public class HostNumaNodeInventory {
    private Integer nodeId;
    private List<Integer> cpus;
    private List<String> distance;

    HostNumaNodeInventory(HostNumaNodeVO hostNumaNode) {
        this.setNodeId(hostNumaNode);
        this.setCpus(hostNumaNode);
        this.setDistance(hostNumaNode);
    }

    public void setNodeId(HostNumaNodeVO hostNumaNodeVO) {
        this.nodeId = hostNumaNodeVO.getNodeID();
    }

    public void setCpus(HostNumaNodeVO hostNumaNodeVO) {
        this.cpus = Arrays
                .stream(StringUtils.deleteWhitespace(hostNumaNodeVO.getNodeCPUs()).split(VmNumaConstant.CPU_SET_SEPARATOR))
                .map(Integer::valueOf).collect(Collectors.toList());
    }

    public void setDistance(HostNumaNodeVO hostNumaNodeVO) {
        this.distance = Arrays
                .stream(StringUtils.deleteWhitespace(hostNumaNodeVO.getNodeDistance()).split(VmNumaConstant.CPU_SET_SEPARATOR))
                .collect(Collectors.toList());
    }

    public boolean checkCpuList(Set<Integer> cpuList) {
        for (Integer cpu : cpuList) {
            if (cpus.stream().noneMatch(k -> k.equals(cpu))) {
                return false;
            }
        }
        return true;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public List<Integer> getCpus() {
        return cpus;
    }

    public List<String> getDistance() {
        return distance;
    }
}
