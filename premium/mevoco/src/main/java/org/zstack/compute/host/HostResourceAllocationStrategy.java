package org.zstack.compute.host;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.allocator.HostAllocatedCpuVO;
import org.zstack.header.allocator.HostAllocatedCpuVO_;
import org.zstack.header.host.HostNUMANode;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HostResourceAllocationStrategy {
    protected String hostUuid;
    protected String scene;
    protected Map<String, HostNUMANode> numaNodes;
    protected List<String> allocatedCpus;
    protected Map<String, Object> addons;

    protected int availableCpuNum = 0;
    protected Long availableMemSize = 0L;
    protected int availableNodeNum = 0;
    protected List<String> needAllocatedCpus = new ArrayList<>();
    protected List<String> cpuBlackList = new ArrayList<>();

    private DatabaseFacade dbf = Platform.getComponentLoader().getComponent(DatabaseFacade.class);
    private static final CLogger logger = Utils.getLogger(HostResourceAllocationStrategy.class);

    public HostResourceAllocationStrategy() {}

    public HostResourceAllocationStrategy(String hostUuid, String scene, Map<String, HostNUMANode> numaNodes, List<String> allocatedCpus) {
        this.hostUuid = hostUuid;
        this.allocatedCpus = allocatedCpus;
        this.numaNodes = numaNodes;
        this.scene = scene;
    }

    public void allocate(int vCpuNum, long memSize, boolean cyclicDistribution) {}

    public void prepareAllocate() {
        this.allocatedCpus = Q.New(HostAllocatedCpuVO.class).select(HostAllocatedCpuVO_.allocatedCPU)
                .eq(HostAllocatedCpuVO_.hostUuid, this.hostUuid).listValues()
                .stream().map(String::valueOf).collect(Collectors.toList());

        this.allocatedCpus.addAll(this.cpuBlackList);
        this.availableNodeNum = this.numaNodes.size();

        Iterator<Map.Entry<String, HostNUMANode>> entries = this.numaNodes.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, HostNUMANode> entry = entries.next();
            this.availableMemSize += entry.getValue().getFree();
            List<String> nodeCpus = new ArrayList<>(entry.getValue().getCpus());
            nodeCpus.removeAll(this.allocatedCpus);
            this.availableCpuNum += nodeCpus.size();
        }
    }

    protected void failedAllocateAndRollback() {
        for (String pCpuId: this.needAllocatedCpus) {
            SQL.New("delete from HostAllocatedCpuVO where hostUuid = :uuid and allocatedCPU = :cpu ")
                    .param("uuid", this.hostUuid).param("cpu", pCpuId).execute();
        }
        this.needAllocatedCpus.clear();
    }

    protected List<String> isPersistentAllocatedCpusToDB(List<String> needAllocatedCpus) {
        List<String> failAllocatedCpus = new ArrayList<>();
        try {
            dbf.persistCollection(needAllocatedCpus.stream().map(this::toHostAllocatedCpuVO).collect(Collectors.toList()));
        } catch (ConstraintViolationException e) {
            logger.warn(e.getMessage());
            failAllocatedCpus.addAll(needAllocatedCpus);
        }

        return failAllocatedCpus;
    }

    protected HostAllocatedCpuVO toHostAllocatedCpuVO(String cpuId) {
        return new HostAllocatedCpuVO(this.hostUuid, cpuId);
    }

    protected boolean isCpuNumEnough(int vCPUNum) {
        if (this.availableCpuNum < vCPUNum) {
            return false;
        } else {
            return true;
        }
    }

    protected boolean isMemSizeEnough(Long memSize) {
        if (this.availableMemSize < memSize) {
            return false;
        } else {
            return true;
        }
    }

    protected void clearAllocatedCPUs() {
        SQL.New("delete from HostAllocatedCpuVO where hostUuid= :uuid ").param("uuid", this.hostUuid).execute();
        this.allocatedCpus.clear();
        this.allocatedCpus.addAll(this.cpuBlackList);
    }

    public List<String> getNewAllocatedCPUs() {
        return this.needAllocatedCpus;
    }

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }

    public Map<String, HostNUMANode> getNumaNodes() {
        return numaNodes;
    }

    public void setNumaNodes(Map<String, HostNUMANode> numaNodes) {
        this.numaNodes = numaNodes;
    }

    public List<String> getAllocatedCpus() {
        return allocatedCpus;
    }

    public void setAllocatedCpus(List<String> allocatedCpus) {
        this.allocatedCpus = allocatedCpus;
    }

    public int getAvailableCpuNum() {
        return availableCpuNum;
    }

    public void setAvailableCpuNum(int availableCpuNum) {
        this.availableCpuNum = availableCpuNum;
    }

    public Long getAvailableMemSize() {
        return availableMemSize;
    }

    public void setAvailableMemSize(Long availableMemSize) {
        this.availableMemSize = availableMemSize;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public int getAvailableNodeNum() {
        return availableNodeNum;
    }

    public void setAvailableNodeNum(int availableNodeNum) {
        this.availableNodeNum = availableNodeNum;
    }

    public Map<String, Object> getAddons() {
        return addons;
    }

    public void setAddons(Map<String, Object> addons) {
        this.addons = addons;
    }

    public List<String> getNeedAllocatedCpus() {
        return needAllocatedCpus;
    }

    public void setNeedAllocatedCpus(List<String> needAllocatedCpus) {
        this.needAllocatedCpus = needAllocatedCpus;
    }

    public List<String> getCpuBlackList() {
        return cpuBlackList;
    }

    public void setCpuBlackList(List<String> cpuBlackList) {
        this.cpuBlackList = cpuBlackList;
    }
}
