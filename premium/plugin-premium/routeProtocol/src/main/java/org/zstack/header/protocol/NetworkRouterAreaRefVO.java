package org.zstack.header.protocol;

import org.zstack.header.network.l3.L3NetworkEO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.ToInventory;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
public class NetworkRouterAreaRefVO implements ToInventory {
    @Id
    @Column
    private String uuid;

    @Column
    private String vRouterUuid;

    @Column
    private String applianceVmType;

    @Column
    @ForeignKey(parentEntityClass = RouterAreaVO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String routerAreaUuid;

    @Column
    @ForeignKey(parentEntityClass = L3NetworkEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String l3NetworkUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getvRouterUuid() {
        return vRouterUuid;
    }

    public void setvRouterUuid(String vRouterUuid) {
        this.vRouterUuid = vRouterUuid;
    }

    public String getRouterAreaUuid() {
        return routerAreaUuid;
    }

    public void setRouterAreaUuid(String routerAreaUuid) {
        this.routerAreaUuid = routerAreaUuid;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
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

    public String getApplianceVmType() {
        return applianceVmType;
    }

    public void setApplianceVmType(String applianceVmType) {
        this.applianceVmType = applianceVmType;
    }
}

