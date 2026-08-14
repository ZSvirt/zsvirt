package org.zstack.storage.device.nvme;

import org.zstack.core.Platform;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.NoView;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ToInventory;
import org.zstack.storage.device.StorageDeviceState;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by MaJin on 2022/8/10.
 */

@Entity
@Table
@AutoDeleteTag
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = NvmeLunVO.class, myField = "uuid", targetField = "nvmeTargetUuid"),
        },
        parents = {
                @EntityGraph.Neighbour(type = NvmeServerVO.class, myField = "nvmeServerUuid", targetField = "uuid")
        }
)
public class NvmeTargetVO extends ResourceVO implements ToInventory {
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name="nvmeTargetUuid", insertable=false, updatable=false)
    @NoView
    private Set<NvmeLunVO> nvmeLuns = new HashSet<>();

    @Column
    @ForeignKey(parentEntityClass = NvmeServerVO.class)
    private String nvmeServerUuid;

    @Column
    private String name;

    @Column
    private String nqn;

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

    public NvmeTargetVO() {
    }

    public NvmeTargetVO(String nqn) {
        this.uuid = Platform.getUuid();
        this.nqn = nqn;
        this.name = String.format("nvme-of-%s", nqn);
        this.state = StorageDeviceState.Enabled.toString();
        this.createDate = new Timestamp(System.currentTimeMillis());
        this.lastOpDate = new Timestamp(System.currentTimeMillis());
    }

    public Set<NvmeLunVO> getNvmeLuns() {
        return nvmeLuns;
    }

    public void setNvmeLuns(Set<NvmeLunVO> nvmeLuns) {
        this.nvmeLuns = nvmeLuns;
    }

    public String getNqn() {
        return nqn;
    }

    public void setNqn(String nqn) {
        this.nqn = nqn;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNvmeServerUuid() {
        return nvmeServerUuid;
    }

    public void setNvmeServerUuid(String nvmeServerUuid) {
        this.nvmeServerUuid = nvmeServerUuid;
    }
}

