package org.zstack.header.vm;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ToInventory;

import javax.persistence.*;

@Entity
@Table
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = TemplatedVmInstanceVO.class, myField = "templatedVmInstanceUuid", targetField = "uuid")
        }
)
public class TemplatedVmInstanceCacheVO implements ToInventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    @ForeignKey(parentEntityClass = TemplatedVmInstanceVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String templatedVmInstanceUuid;

    @Column
    private String cacheVmInstanceUuid;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTemplatedVmInstanceUuid() {
        return templatedVmInstanceUuid;
    }

    public void setTemplatedVmInstanceUuid(String templatedVmInstanceUuid) {
        this.templatedVmInstanceUuid = templatedVmInstanceUuid;
    }

    public String getCacheVmInstanceUuid() {
        return cacheVmInstanceUuid;
    }

    public void setCacheVmInstanceUuid(String cacheVmInstanceUuid) {
        this.cacheVmInstanceUuid = cacheVmInstanceUuid;
    }
}
