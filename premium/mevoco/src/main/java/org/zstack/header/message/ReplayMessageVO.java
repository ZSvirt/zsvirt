package org.zstack.header.message;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by MaJin on 2020/10/20.
 */

@Entity
@Table
public class ReplayMessageVO {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    private String msgDump;

    @Column
    private String locationType;

    @Column
    private String locationUuid;

    @Column
    private String groupUuid;

    @Column
    private String resourceUuid;

    @Column
    private String manageJobUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getMsgDump() {
        return msgDump;
    }

    public void setMsgDump(String msgDump) {
        this.msgDump = msgDump;
    }

    public String getLocationType() {
        return locationType;
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }

    public String getLocationUuid() {
        return locationUuid;
    }

    public void setLocationUuid(String locationUuid) {
        this.locationUuid = locationUuid;
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

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getManageJobUuid() {
        return manageJobUuid;
    }

    public void setManageJobUuid(String manageJobUuid) {
        this.manageJobUuid = manageJobUuid;
    }
}
