package org.zstack.header.search;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table
public class DeleteVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    private String voName;

    @Column
    private String uuid;

    @Column
    private String foreignVOName;

    @Column
    private String foreignVOUuid;

    @Column
    private String foreignVOToDeleteName;

    @Column
    private String foreignVOToDeleteUuid;

    @Column
    private Date deletedDate;

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

    public Date getDeletedDate() {
        return deletedDate;
    }

    public void setDeletedDate(Date deletedDate) {
        this.deletedDate = deletedDate;
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

    public String getForeignVOToDeleteName() {
        return foreignVOToDeleteName;
    }

    public void setForeignVOToDeleteName(String foreignVOToDeleteName) {
        this.foreignVOToDeleteName = foreignVOToDeleteName;
    }

    public String getForeignVOToDeleteUuid() {
        return foreignVOToDeleteUuid;
    }

    public void setForeignVOToDeleteUuid(String foreignVOToDeleteUuid) {
        this.foreignVOToDeleteUuid = foreignVOToDeleteUuid;
    }
}
