package org.zstack.header.vpc.ha;

import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
public class VpcHaGroupNetworkServiceRefVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    @ForeignKey(parentEntityClass = VpcHaGroupVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String vpcHaRouterUuid;

    @Column
    private String networkServiceName;

    @Column
    private String networkServiceUuid;

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

    public String getVpcHaRouterUuid() {
        return vpcHaRouterUuid;
    }

    public void setVpcHaRouterUuid(String vpcHaRouterUuid) {
        this.vpcHaRouterUuid = vpcHaRouterUuid;
    }

    public String getNetworkServiceName() {
        return networkServiceName;
    }

    public void setNetworkServiceName(String networkServiceName) {
        this.networkServiceName = networkServiceName;
    }

    public String getNetworkServiceUuid() {
        return networkServiceUuid;
    }

    public void setNetworkServiceUuid(String networkServiceUuid) {
        this.networkServiceUuid = networkServiceUuid;
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
