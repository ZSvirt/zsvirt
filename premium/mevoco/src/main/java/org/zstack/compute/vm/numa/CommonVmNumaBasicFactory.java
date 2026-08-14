package org.zstack.compute.vm.numa;

import org.zstack.compute.vm.MevocoVmSystemTags;
import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.host.HostNumaNodeVO;
import org.zstack.header.host.HostNumaNodeVO_;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

/**
 * Created by longtao.wu@zstack.io on 21/12/09
 */
public class CommonVmNumaBasicFactory implements VmNumaFactory {
    private final Map<Integer, VmNumaNodeInventory> vmNumaNodeByHostNumaNodeIdMap;
    private String hostUuid;
    private Integer vmCpuSum;
    private String vmUuid;
    private Long vmMemorySize;

    CommonVmNumaBasicFactory(String hostUuid, VmInstanceSpec spec) {
        this.setVmUuid(spec.getVmInventory().getUuid());
        this.setVmCpuSum(spec.getVmInventory().getCpuNum());
        this.setVmMemorySize(spec.getVmInventory().getMemorySize());
        this.setHostUuid(hostUuid);
        this.hostUuid = hostUuid;
        vmNumaNodeByHostNumaNodeIdMap = new HashMap<>();
    }

    private static void baseCheck(String hostUuid, String vmUuid) {
        if (!Q.New(HostNumaNodeVO.class).eq(HostNumaNodeVO_.hostUuid, hostUuid).isExists()) {
            throw new OperationFailureException(operr(Platform.i18n("vm[%s] start fail,because numa is enable but host[%s] not have numa node", vmUuid, hostUuid)));
        }
        if (!MevocoVmSystemTags.VM_CPU_PINNING.hasTag(vmUuid)) {
            throw new OperationFailureException(operr(Platform.i18n("vm[%s] start fail,because numa is enable but cpu not pin", vmUuid)));
        }
    }


    private void setVmNumaNodeCpus(Map<Integer, Set<Integer>> hostCpuListByNumaVmCpuMap, Map<Integer, HostNumaNodeInventory> hostNumaNodes) {
        AtomicInteger index = new AtomicInteger();
        for (Map.Entry<Integer, Set<Integer>> entry : hostCpuListByNumaVmCpuMap.entrySet()) {
            HostNumaNodeInventory pNode = hostNumaNodes.get(entry.getValue().iterator().next());
            vmNumaNodeByHostNumaNodeIdMap.computeIfAbsent(pNode.getNodeId(), k -> new VmNumaNodeInventory(pNode.getDistance(),
                    index.getAndIncrement(), pNode.getNodeId())).addCpus(entry.getKey());
        }
    }

    private List<Integer> sortHostNodeIds(){
        Integer[] hostNodeIdArray = new Integer[vmNumaNodeByHostNumaNodeIdMap.size()];
        for (Map.Entry<Integer, VmNumaNodeInventory> entry : vmNumaNodeByHostNumaNodeIdMap.entrySet()) {
            hostNodeIdArray[entry.getValue().getNodeID()] = entry.getKey();
        }
        return Arrays.stream(hostNodeIdArray).collect(Collectors.toList());
    }

    private void setVmNumaNodeMapMemoryAndDistance() {
        long memory = vmMemorySize / VmNumaConstant.MEMORY_FINENESS;
        List<Integer> hostNodeIds = sortHostNodeIds();
        long memoryPerVCpu = memory / vmCpuSum * VmNumaConstant.MEMORY_FINENESS;
        long offset = memory % vmCpuSum * VmNumaConstant.MEMORY_FINENESS;
        for (Integer hostNodeId : hostNodeIds) {
            vmNumaNodeByHostNumaNodeIdMap.get(hostNodeId).setMemorySize(memoryPerVCpu);
            vmNumaNodeByHostNumaNodeIdMap.get(hostNodeId).setDistance(hostNodeIds);
        }
        vmNumaNodeByHostNumaNodeIdMap.get(hostNodeIds.get(0)).addMemorySize(offset);
    }

    @Override
    public List<VmNumaNodeInventory> generator() {
        baseCheck(hostUuid, vmUuid);
        Map<Integer, HostNumaNodeInventory> hostNumaNodes = getHostNumaNodesFromHostNumaNodeVO(hostUuid);

        VmNumaCpuPinInventory vmNumaCpuPinInventory = new VmNumaCpuPinInventory();
        vmNumaCpuPinInventory.setHostCpuListByVmNumaCpuMap(vmUuid);
        vmNumaCpuPinInventory.checkBindingConditions(vmUuid, vmCpuSum, hostNumaNodes);

        setVmNumaNodeCpus(vmNumaCpuPinInventory.getHostCpuListByVmNumaCpuMap(), hostNumaNodes);
        setVmNumaNodeMapMemoryAndDistance();
        return new ArrayList<>(vmNumaNodeByHostNumaNodeIdMap.values());
    }

    @Override
    public VmNumaConstant.ClusterType getType() {
        return VmNumaConstant.ClusterType.COMMON_CLUSTER;
    }


    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public void setVmCpuSum(Integer vmCpuSum) {
        this.vmCpuSum = vmCpuSum;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public void setVmMemorySize(Long vmMemorySize) {
        this.vmMemorySize = vmMemorySize;
    }

}
