package org.zstack.appliancevm;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.network.l3.L3NetworkEO;
import org.zstack.header.vm.VmInstanceEO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;

import javax.persistence.*;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 12:11 AM
 * To change this template use File | Settings | File Templates.
 */
@Entity
@Table
@PrimaryKeyJoinColumn(name="uuid", referencedColumnName="uuid")
@EO(EOClazz = VmInstanceEO.class, needView = false)
public class ApplianceVmVO extends VmInstanceVO {
    @Column
    private String applianceVmType;
    @Column
    @ForeignKey(parentEntityClass = L3NetworkEO.class, onDeleteAction = ReferenceOption.RESTRICT)
    private String managementNetworkUuid;
    @Column
    @ForeignKey(parentEntityClass = L3NetworkEO.class)
    private String defaultRouteL3NetworkUuid;
    @Column
    @Enumerated(EnumType.STRING)
    private ApplianceVmStatus status = ApplianceVmStatus.Connecting;
    @Column
    private int agentPort;

    @Column
    @Enumerated(EnumType.STRING)
    private ApplianceVmHaStatus haStatus;

    public ApplianceVmVO(ApplianceVmVO other) {
        super(other);
        this.applianceVmType = other.applianceVmType;
        this.managementNetworkUuid = other.managementNetworkUuid;
        this.defaultRouteL3NetworkUuid = other.defaultRouteL3NetworkUuid;
        this.status = other.status;
        this.agentPort = other.agentPort;
        this.haStatus = other.haStatus;
    }

    public ApplianceVmVO() {
    }

    public int getAgentPort() {
        return agentPort;
    }

    public void setAgentPort(int agentPort) {
        this.agentPort = agentPort;
    }

    public String getDefaultRouteL3NetworkUuid() {
        return defaultRouteL3NetworkUuid;
    }

    public void setDefaultRouteL3NetworkUuid(String defaultRouteL3NetworkUuid) {
        this.defaultRouteL3NetworkUuid = defaultRouteL3NetworkUuid;
    }

    public String getApplianceVmType() {
        return applianceVmType;
    }

    public void setApplianceVmType(String applianceVmType) {
        this.applianceVmType = applianceVmType;
    }

    public String getManagementNetworkUuid() {
        return managementNetworkUuid;
    }

    public void setManagementNetworkUuid(String managementNetworkUuid) {
        this.managementNetworkUuid = managementNetworkUuid;
    }

    public ApplianceVmStatus getStatus() {
        return status;
    }

    public void setStatus(ApplianceVmStatus status) {
        this.status = status;
    }

    public ApplianceVmHaStatus getHaStatus() {
        return haStatus;
    }

    public void setHaStatus(ApplianceVmHaStatus haStatus) {
        this.haStatus = haStatus;
    }

    public boolean isHaEnabled() {
        return !ApplianceVmHaStatus.NoHa.equals(haStatus);
    }
}
