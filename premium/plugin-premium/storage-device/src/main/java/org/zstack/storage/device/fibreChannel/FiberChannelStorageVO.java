package org.zstack.storage.device.fibreChannel;

import org.zstack.core.Platform;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.NoView;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ToInventory;
import org.zstack.storage.device.StorageDeviceState;
import org.zstack.storage.device.iscsi.IscsiServerClusterRefVO;
import org.zstack.storage.device.iscsi.IscsiTargetVO;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

/**
 * Create by weiwang at 2018/10/18
 */
@Entity
@Table
@AutoDeleteTag
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = FiberChannelLunVO.class, myField = "uuid", targetField = "fiberChannelStorageUuid"),
        }
)
public class FiberChannelStorageVO extends ResourceVO implements ToInventory {
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name="fiberChannelStorageUuid", insertable=false, updatable=false)
    @NoView
    private Set<FiberChannelLunVO> fiberChannelLuns = new HashSet<FiberChannelLunVO>();

    @Column
    private String name;

    @Column
    private String wwnn;

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

    public FiberChannelStorageVO() {
    }

    public FiberChannelStorageVO(String wwnn) {
        this.uuid = Platform.getUuid();
        this.wwnn = wwnn;
        this.name = String.format("fc-san-%s", wwnn);
        this.state = StorageDeviceState.Enabled.toString();
        this.createDate = new Timestamp(System.currentTimeMillis());
        this.lastOpDate = new Timestamp(System.currentTimeMillis());
    }

    public Set<FiberChannelLunVO> getFiberChannelLuns() {
        return fiberChannelLuns;
    }

    public void setFiberChannelLuns(Set<FiberChannelLunVO> fiberChannelLuns) {
        this.fiberChannelLuns = fiberChannelLuns;
    }

    public String getWwnn() {
        return wwnn;
    }

    public void setWwnn(String wwnn) {
        this.wwnn = wwnn;
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
}

