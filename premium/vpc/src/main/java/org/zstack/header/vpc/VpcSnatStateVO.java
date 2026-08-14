package org.zstack.header.vpc;

import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
public class VpcSnatStateVO {
    @Id
    @Column
    private String uuid;

    @Column
    @ForeignKey(parentEntityClass = VpcRouterVmVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String vpcUuid;

    @Column
    private String l3NetworkUuid;

    @Column
    private String state;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getVpcUuid() {
        return vpcUuid;
    }

    public void setVpcUuid(String vpcUuid) {
        this.vpcUuid = vpcUuid;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
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

    public VpcSnatStateVO() {}

    public VpcSnatStateVO(String uuid, String vpcUuid, String l3NetworkUuid, String state) {
        this.uuid = uuid;
        this.vpcUuid = vpcUuid;
        this.l3NetworkUuid = l3NetworkUuid;
        this.state = state;
    }
}
