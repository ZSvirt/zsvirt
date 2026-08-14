package org.zstack.header.search;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table
public class InsertVO {
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
    private Date insertDate;

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

    public Date getInsertDate() {
        return insertDate;
    }

    public void setInsertDate(Date insertDate) {
        this.insertDate = insertDate;
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
