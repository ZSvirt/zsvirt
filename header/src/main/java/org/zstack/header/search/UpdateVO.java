package org.zstack.header.search;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table
public class UpdateVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    private String voName;

    @Column
    private String uuid;

    @Column
    private Date updateDate;

    @Column
    private String foreignVOName;

    @Column
    private String foreignVOUuid;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getVoName() {
        return voName;
    }

    public void setVoName(String voName) {
        this.voName = voName;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    public String getForeignVOName() {
        return foreignVOName;
    }

    public void setForeignVOName(String foreignVOName) {
        this.foreignVOName = foreignVOName;
    }

    public String getForeignVOUuid() {
        return foreignVOUuid;
    }

    public void setForeignVOUuid(String foreignVOUuid) {
        this.foreignVOUuid = foreignVOUuid;
    }
}
