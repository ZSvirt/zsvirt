package org.zstack.externalbackup;

import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by MaJin on 2019/11/30.
 */
@Table
@Entity
@BaseResource
public class ExternalBackupVO extends ResourceVO {
    @Column
    protected String name;
    @Column
    protected String description;
    @Column
    protected String type;
    @Column
    protected long totalSize;
    @Column
    protected String version;
    @Column
    @Enumerated(EnumType.STRING)
    protected ExternalBackupState state;
    @Column
    protected String installPath;
    @Column
    protected Timestamp createDate;
    @Column
    protected Timestamp lastOpDate;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public String getInstallPath() {
        return installPath;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public ExternalBackupState getState() {
        return state;
    }

    public void setState(ExternalBackupState state) {
        this.state = state;
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

    public ExternalBackupVO(ExternalBackupVO vo) {
        super();
        this.uuid = vo.getUuid();
        this.name = vo.getName();
        this.description = vo.getDescription();
        this.totalSize = vo.getTotalSize();
        this.version = vo.getVersion();
        this.type = vo.getType();
        this.state = vo.getState();
        this.createDate = vo.getCreateDate();
        this.lastOpDate = vo.getLastOpDate();
    }

    public ExternalBackupVO() {
        super();
    }
}
