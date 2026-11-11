package org.zstack.network.zns.entity;

import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
@Entity
@Table
public class ZnsSegmentRefVO {
    @Id
    @Column
    private String l2PortGroupUuid;
    @Column(nullable = false)
    private String znsInfoUuid;
    @Column(nullable = false)
    private String znsSegmentUuid;
    @Column
    private String znsTransportZoneUuid;
    @Column
    private String tenantRouterUuid;
    @Column
    private String transportType;
    @Column
    private Integer virtualNetworkId;
    @Column
    private Long configVersion;
    @Column
    private String state;
    @Column
    private String lastOperation;
    @Column
    private String lastError;
    @Column
    private Timestamp lastSyncDate;

    public String getL2PortGroupUuid() {
        return l2PortGroupUuid;
    }

    public void setL2PortGroupUuid(String l2PortGroupUuid) {
        this.l2PortGroupUuid = l2PortGroupUuid;
    }

    public String getZnsInfoUuid() {
        return znsInfoUuid;
    }

    public void setZnsInfoUuid(String znsInfoUuid) {
        this.znsInfoUuid = znsInfoUuid;
    }

    public String getZnsSegmentUuid() {
        return znsSegmentUuid;
    }

    public void setZnsSegmentUuid(String znsSegmentUuid) {
        this.znsSegmentUuid = znsSegmentUuid;
    }

    public String getZnsTransportZoneUuid() {
        return znsTransportZoneUuid;
    }

    public void setZnsTransportZoneUuid(String znsTransportZoneUuid) {
        this.znsTransportZoneUuid = znsTransportZoneUuid;
    }

    public String getTenantRouterUuid() {
        return tenantRouterUuid;
    }

    public void setTenantRouterUuid(String tenantRouterUuid) {
        this.tenantRouterUuid = tenantRouterUuid;
    }

    public String getTransportType() {
        return transportType;
    }

    public void setTransportType(String transportType) {
        this.transportType = transportType;
    }

    public Integer getVirtualNetworkId() {
        return virtualNetworkId;
    }

    public void setVirtualNetworkId(Integer virtualNetworkId) {
        this.virtualNetworkId = virtualNetworkId;
    }

    public Long getConfigVersion() {
        return configVersion;
    }

    public void setConfigVersion(Long configVersion) {
        this.configVersion = configVersion;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getLastOperation() {
        return lastOperation;
    }

    public void setLastOperation(String lastOperation) {
        this.lastOperation = lastOperation;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public Timestamp getLastSyncDate() {
        return lastSyncDate;
    }

    public void setLastSyncDate(Timestamp lastSyncDate) {
        this.lastSyncDate = lastSyncDate;
    }
}
