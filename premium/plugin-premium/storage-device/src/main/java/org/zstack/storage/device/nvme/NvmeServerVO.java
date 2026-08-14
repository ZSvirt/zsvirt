package org.zstack.storage.device.nvme;

import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.NoView;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ToInventory;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table
@AutoDeleteTag
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = NvmeTargetVO.class, myField = "uuid", targetField = "nvmeServerUuid"),
                @EntityGraph.Neighbour(type = NvmeServerClusterRefVO.class, myField = "uuid", targetField = "nvmeServerUuid")
        }
)
public class NvmeServerVO extends ResourceVO implements ToInventory {
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name="nvmeServerUuid", insertable=false, updatable=false)
    @NoView
    private Set<NvmeServerClusterRefVO> nvmeClusterRefs = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name="nvmeServerUuid", insertable=false, updatable=false)
    @NoView
    private Set<NvmeTargetVO> nvmeTargets = new HashSet<>();

    @Column
    private String name;

    @Column
    private String ip;

    @Column
    private Integer port;

    @Column
    private String transport;

    @Column
    private String state;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public Set<NvmeServerClusterRefVO> getNvmeClusterRefs() {
        return nvmeClusterRefs;
    }

    public void setNvmeClusterRefs(Set<NvmeServerClusterRefVO> nvmeClusterRefs) {
        this.nvmeClusterRefs = nvmeClusterRefs;
    }

    public Set<NvmeTargetVO> getNvmeTargets() {
        return nvmeTargets;
    }

    public void setNvmeTargets(Set<NvmeTargetVO> nvmeTargets) {
        this.nvmeTargets = nvmeTargets;
    }

    public Set<NvmeLunVO> getNvmeLuns() {
        Set<NvmeLunVO> result = new HashSet<>();
        for (NvmeTargetVO targetVO : getNvmeTargets()) {
            result.addAll(targetVO.getNvmeLuns());
        }
        return result;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public String getTransport() {
        return transport;
    }

    public void setTransport(String transport) {
        this.transport = transport;
    }
}
