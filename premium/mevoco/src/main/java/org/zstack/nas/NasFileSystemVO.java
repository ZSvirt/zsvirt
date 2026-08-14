package org.zstack.nas;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ToInventory;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by mingjian.deng on 2018/3/5.
 */
@Entity
@Table
@BaseResource
public class NasFileSystemVO extends ResourceVO implements ToInventory, OwnedByAccount {
    @Column
    @Enumerated(EnumType.STRING)
    private NasProtocolType protocol;
    @Column
    private String name;
    @Column
    private String description;
    @Column
    protected String fileSystemId;
    @Column
    private String type;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;

    @Transient
    private String accountUuid;

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public NasProtocolType getProtocol() {
        return protocol;
    }

    public void setProtocol(NasProtocolType protocol) {
        this.protocol = protocol;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public String getFileSystemId() {
        return fileSystemId;
    }

    public void setFileSystemId(String fileSystemId) {
        this.fileSystemId = fileSystemId;
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
