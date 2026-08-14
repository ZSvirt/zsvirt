package org.zstack.header.cbt;


import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.NoView;
import org.zstack.header.vo.ToInventory;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = CbtTaskResourceRefVO.class, myField = "uuid", targetField = "taskUuid")
        }
)
public class CbtTaskVO implements ToInventory {
    @Column
    @Id
    private String uuid;

    @Column
    private String name;

    @Column
    private String description;

    @Column
    @Enumerated(EnumType.STRING)
    private CbtTaskStatus status;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "taskUuid", insertable = false, updatable = false)
    @NoView
    private Set<CbtTaskResourceRefVO> resourceRefs = new HashSet<>();

    public CbtTaskVO() {
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public CbtTaskStatus getStatus() {
        return status;
    }

    public void setStatus(CbtTaskStatus status) {
        this.status = status;
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

    public Set<CbtTaskResourceRefVO> getResourceRefs() {
        return resourceRefs;
    }

    public void setResourceRefs(Set<CbtTaskResourceRefVO> resourceRefs) {
        this.resourceRefs = resourceRefs;
    }
}
