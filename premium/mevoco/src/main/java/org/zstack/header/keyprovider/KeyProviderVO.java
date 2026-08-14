package org.zstack.header.keyprovider;

import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ToInventory;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table
@BaseResource
@AutoDeleteTag
public class KeyProviderVO extends ResourceVO implements ToInventory {
    @Column
    private String name;

    @Column
    private String description;

    @Column
    @Enumerated(EnumType.STRING)
    private KeyProviderType type;

    @Column
    private boolean connected;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public KeyProviderVO() {
    }

    public KeyProviderVO(KeyProviderVO vo) {
        this.setName(vo.getName());
        this.setDescription(vo.getDescription());
        this.setType(vo.getType());
        this.setConnected(vo.isConnected());
        this.setUuid(vo.getUuid());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

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

    public KeyProviderType getType() {
        return type;
    }

    public void setType(KeyProviderType type) {
        this.type = type;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
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
