package org.zstack.header.host;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by LiangHanYu on 2021/8/25 13:56
 */
@Entity
@Table
public class CpuFeaturesHistoryVO {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column
    private String srcHostUuid;
    @Column
    private String dstHostUuid;
    @Column
    private String srcCpuModelName;
    @Column
    private boolean supportLiveMigration;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;

    public String getSrcCpuModelName() {
        return srcCpuModelName;
    }

    public void setSrcCpuModelName(String srcCpuModelName) {
        this.srcCpuModelName = srcCpuModelName;
    }

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSrcHostUuid() {
        return srcHostUuid;
    }

    public void setSrcHostUuid(String srcHostUuid) {
        this.srcHostUuid = srcHostUuid;
    }

    public String getDstHostUuid() {
        return dstHostUuid;
    }

    public void setDstHostUuid(String dstHostUuid) {
        this.dstHostUuid = dstHostUuid;
    }

    public boolean isSupportLiveMigration() {
        return supportLiveMigration;
    }

    public void setSupportLiveMigration(boolean supportLiveMigration) {
        this.supportLiveMigration = supportLiveMigration;
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
