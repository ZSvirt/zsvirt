package org.zstack.storage.device.iscsi;

import org.zstack.header.rest.APINoSee;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.*;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

/**
 * Create by weiwang at 2018/8/1
 */
@Entity
@Table
@AutoDeleteTag
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = IscsiServerVO.class, myField = "iscsiServerUuid", targetField = "uuid")
        }
)
public class IscsiTargetVO extends ResourceVO implements ToInventory {
    @Column
    @ForeignKey(parentEntityClass = IscsiServerVO.class)
    private String iscsiServerUuid;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name="iscsiTargetUuid", insertable=false, updatable=false)
    @NoView
    private Set<IscsiLunVO> iscsiLuns = new HashSet<IscsiLunVO>();

    @Column
    private String iqn;

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

    public String getIscsiServerUuid() {
        return iscsiServerUuid;
    }

    public void setIscsiServerUuid(String iscsiServerUuid) {
        this.iscsiServerUuid = iscsiServerUuid;
    }

    public String getIqn() {
        return iqn;
    }

    public void setIqn(String iqn) {
        this.iqn = iqn;
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

    public Set<IscsiLunVO> getIscsiLuns() {
        return iscsiLuns;
    }

    public void setIscsiLuns(Set<IscsiLunVO> iscsiLunVOS) {
        this.iscsiLuns = iscsiLunVOS;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
