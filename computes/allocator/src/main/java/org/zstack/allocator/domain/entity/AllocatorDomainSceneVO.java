package org.zstack.allocator.domain.entity;

import org.zstack.header.allocator.AllocatorDomainScene;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ToInventory;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = AllocatorDomainVO.class, myField = "domainUuid", targetField = "uuid")
        }
)
public class AllocatorDomainSceneVO implements ToInventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @Column
    @ForeignKey(parentEntityClass = AllocatorDomainVO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String domainUuid;

    @Column
    @Enumerated(EnumType.STRING)
    private AllocatorDomainScene scene;

    @Column
    private Timestamp createDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDomainUuid() {
        return domainUuid;
    }

    public void setDomainUuid(String domainUuid) {
        this.domainUuid = domainUuid;
    }

    public AllocatorDomainScene getScene() {
        return scene;
    }

    public void setScene(AllocatorDomainScene scene) {
        this.scene = scene;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }
}
