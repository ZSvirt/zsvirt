package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.zstack.header.network.l2.L2NetworkEO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by weiwang on 08/03/2017.
 */
@Entity
@Table
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = VxlanNetworkPoolVO.class, myField = "l2NetworkUuid", targetField = "uuid")
        }
)
public class VniRangeVO extends ResourceVO {
    @Column
    private String name;

    @Column
    private String description;

    @Column
    private Integer startVni;

    @Column
    private Integer endVni;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @Column
    @ForeignKey(parentEntityClass = L2NetworkEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String l2NetworkUuid;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }

    public Integer getStartVni() {
        return startVni;
    }

    public void setStartVni(Integer startVni) {
        this.startVni = startVni;
    }

    public Integer getEndVni() {
        return endVni;
    }

    public void setEndVni(Integer endVni) {
        this.endVni = endVni;
    }

    public int size() {
        return (getEndVni() - getStartVni() + 1);
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
}
