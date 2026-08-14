package org.zstack.compute.vm.numa;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.vm.VmInstanceNumaNodeVO;
import org.zstack.header.vm.VmInstanceSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by longtao.wu@zstack.io on 21/12/01
 */
public class VmNumaNodeInventory {
    private Integer nodeID;
    private final List<Integer> cpuList = new ArrayList<>();
    private Long memorySize = 0L;
    private List<String> distance;
    private final Integer hostNumaNodeId;

    VmNumaNodeInventory(List<String> distance, Integer nodeID, Integer hostNumaNodeId) {
        this.distance = distance;
        this.nodeID = nodeID;
        this.hostNumaNodeId = hostNumaNodeId;
    }

    static class VmNumaNodesTO {
        Integer nodeID;
        String cpus;
        String memorySize;
        List<String> distance;
        Integer hostNodeID;

        static VmNumaNodesTO valueOf(VmNumaNodeInventory vmNode) {
            VmNumaNodesTO result = new VmNumaNodesTO();
            result.nodeID = vmNode.getNodeID();
            result.cpus = vmNode.getStringCpus();
            result.memorySize = vmNode.getMemorySize().toString();
            result.distance = vmNode.getDistance();
            result.hostNodeID = vmNode.getHostNumaNodeId();
            return result;
        }
    }

    public Integer getNodeID() {
        return nodeID;
    }

    public String getStringCpus() {
        return StringUtils.join(cpuList.toArray(), VmNumaConstant.CPU_SET_SEPARATOR);
    }

    public Long getMemorySize() {
        return memorySize;
    }

    public List<String> getDistance() {
        return distance;
    }

    public Integer getHostNumaNodeId() {
        return hostNumaNodeId;
    }


    public void setNodeID(Integer nodeID) {
        this.nodeID = nodeID;
    }

    public void addCpus(Integer cpu) {
        this.cpuList.add(cpu);
    }

    public void setMemorySize(Long memorySize) {
        this.memorySize = memorySize * cpuList.size();
    }

    public void addMemorySize(Long offset) {
        this.memorySize += offset;
    }

    public void setDistance(List<Integer> indexList) {
        List<String> newDistance = new ArrayList<>();
        indexList.forEach(k -> addDistance(this.distance, newDistance, k));
        this.distance = newDistance;
    }

    private static void addDistance(List<String> dtOrigin, List<String> dtTarget, Integer index) {
        dtTarget.add(dtOrigin.get(index));
    }

    public static void persistVmNumaNodes(List<VmNumaNodeInventory> vmNodes, VmInstanceSpec spec, DatabaseFacade dbf) {
        List<VmInstanceNumaNodeVO> vmNUMAs = new ArrayList<>();
        for (VmNumaNodeInventory node : vmNodes) {
            VmInstanceNumaNodeVO vmNumaNode = new VmInstanceNumaNodeVO();
            vmNumaNode.setVmUuid(spec.getVmInventory().getUuid());
            vmNumaNode.setvNodeID(node.getNodeID());
            vmNumaNode.setvNodeCPUs(node.getStringCpus());
            vmNumaNode.setvNodeMemSize(node.getMemorySize());
            vmNumaNode.setvNodeDistance(StringUtils.join(node.getDistance().toArray(),
                    VmNumaConstant.CPU_SET_SEPARATOR));
            vmNumaNode.setpNodeID(node.getHostNumaNodeId());
            vmNUMAs.add(vmNumaNode);
        }
        dbf.persistCollection(vmNUMAs);
    }

    public static List<VmNumaNodesTO> getVmNumaNodesTOListFromVmNumaNodeList(List<VmNumaNodeInventory> vmNodes) {
        return vmNodes
                .parallelStream()
                .map(VmNumaNodesTO::valueOf)
                .collect(Collectors.toList());
    }

}
