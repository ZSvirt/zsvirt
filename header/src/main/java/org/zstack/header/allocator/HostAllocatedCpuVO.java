package org.zstack.header.allocator;

import org.zstack.header.host.HostEO;
import org.zstack.header.host.HostVO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;

import javax.persistence.*;

@Entity
@Table
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = HostVO.class, myField = "hostUuid", targetField = "uuid")
        }
)
public class HostAllocatedCpuVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @ForeignKey(parentEntityClass = HostEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String hostUuid;

    @Column
    private Integer allocatedCPU;

    public HostAllocatedCpuVO() {}

    public HostAllocatedCpuVO(String hostUuid, String CPUID) {
        this.hostUuid = hostUuid;
        this.allocatedCPU = Integer.parseInt(CPUID);
    }

    public HostAllocatedCpuVO(String hostUuid, Integer CPUID) {
        this.hostUuid = hostUuid;
        this.allocatedCPU = CPUID;
    }

    public Integer getAllocatedCPU() {
        return allocatedCPU;
    }

    public void setAllocatedCPU(Integer allocatedCPU) {
        this.allocatedCPU = allocatedCPU;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
