package org.zstack.header.resourceattribute.entity;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.Index;
import org.zstack.header.vo.NoView;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = ResourceAttributeKeyVO.class, myField = "keyUuid", targetField = "uuid"),
        }
)
public class ResourceAttributeValueVO {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column
    @Index
    @ForeignKey(parentEntityClass = ResourceAttributeKeyVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String keyUuid;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "keyUuid", referencedColumnName = "uuid", insertable = false, updatable = false)
    @NoView
    private ResourceAttributeKeyVO key;

    @Column
    private String value;

    @Column
    private String resourceUuid;

    @Column
    private String resourceType;

    @Column
    private Timestamp createDate;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getKeyUuid() {
        return keyUuid;
    }

    public void setKeyUuid(String keyUuid) {
        this.keyUuid = keyUuid;
    }

    public ResourceAttributeKeyVO getKey() {
        return key;
    }

    public void setKey(ResourceAttributeKeyVO key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }
}
