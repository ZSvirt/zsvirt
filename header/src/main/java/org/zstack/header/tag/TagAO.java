package org.zstack.header.tag;

import org.zstack.header.vo.Index;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 */
@MappedSuperclass
public class TagAO {
    @Id
    @Column
    @Index
    private String uuid;

    @Column
    @Index
    private String resourceUuid;

    @Column
    @Index
    private String resourceType;

    @Column
    @Index(length = 128)
    private String tag;

    @Column
    @Enumerated(EnumType.STRING)
    @Index
    private TagType type;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public TagAO(TagAO other) {
        this.uuid = other.uuid;
        this.resourceUuid = other.resourceUuid;
        this.resourceType = other.resourceType;
        this.tag = other.tag;
        this.type = other.type;
        this.createDate = other.createDate;
        this.lastOpDate = other.lastOpDate;
    }

    public TagAO() {
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
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

    public TagType getType() {
        return type;
    }

    public void setType(TagType type) {
        this.type = type;
    }
}
