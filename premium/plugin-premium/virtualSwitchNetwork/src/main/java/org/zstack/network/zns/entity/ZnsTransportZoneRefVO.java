package org.zstack.network.zns.entity;

import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
@Entity
@Table
public class ZnsTransportZoneRefVO {
    @Id
    @Column
    private String vSwitchUuid;
    @Column(nullable = false)
    private String znsInfoUuid;
    @Column(nullable = false)
    private String znsTransportZoneUuid;
    @Column
    private String name;
    @Column
    private String description;
    @Column
    private String transportType;
    @Column
    private String physicalNetwork;
    @Column
    private String status;
    @Column
    private boolean defaultZone;
    @Column
    private Timestamp lastSyncDate;

    public String getvSwitchUuid() {
        return vSwitchUuid;
    }

    public void setvSwitchUuid(String vSwitchUuid) {
        this.vSwitchUuid = vSwitchUuid;
    }

    public String getZnsInfoUuid() {
        return znsInfoUuid;
    }

    public void setZnsInfoUuid(String znsInfoUuid) {
        this.znsInfoUuid = znsInfoUuid;
    }

    public String getZnsTransportZoneUuid() {
        return znsTransportZoneUuid;
    }

    public void setZnsTransportZoneUuid(String znsTransportZoneUuid) {
        this.znsTransportZoneUuid = znsTransportZoneUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTransportType() {
        return transportType;
    }

    public void setTransportType(String transportType) {
        this.transportType = transportType;
    }

    public String getPhysicalNetwork() {
        return physicalNetwork;
    }

    public void setPhysicalNetwork(String physicalNetwork) {
        this.physicalNetwork = physicalNetwork;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isDefaultZone() {
        return defaultZone;
    }

    public void setDefaultZone(boolean defaultZone) {
        this.defaultZone = defaultZone;
    }

    public Timestamp getLastSyncDate() {
        return lastSyncDate;
    }

    public void setLastSyncDate(Timestamp lastSyncDate) {
        this.lastSyncDate = lastSyncDate;
    }
}
