package org.zstack.header.network.l3;

import org.zstack.header.network.l2.L2NetworkEO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.Index;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.zone.ZoneEO;

import javax.persistence.*;
import java.sql.Timestamp;

@MappedSuperclass
public class L3NetworkAO extends ResourceVO {
    @Column
    @Index
    private String name;

    @Column
    private String description;

    @Column
    @Enumerated(EnumType.STRING)
    private L3NetworkState state;

    @Column
    private String type;

    @Column
    @ForeignKey(parentEntityClass = ZoneEO.class, onDeleteAction = ReferenceOption.RESTRICT)
    private String zoneUuid;

    @Column
    @ForeignKey(parentEntityClass = L2NetworkEO.class, onDeleteAction = ReferenceOption.RESTRICT)
    private String l2NetworkUuid;

    @Column
    private boolean system;

    @Column
    private String dnsDomain;

    @Column
    private Integer ipVersion;

    @Column
    private Boolean enableIPAM = Boolean.TRUE;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @Column
    @Enumerated(EnumType.STRING)
    private L3NetworkCategory category;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getDnsDomain() {
        return dnsDomain;
    }

    public void setDnsDomain(String domainName) {
        this.dnsDomain = domainName;
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
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

    public L3NetworkState getState() {
        return state;
    }

    public void setState(L3NetworkState state) {
        this.state = state;
    }

    public L3NetworkCategory getCategory() {
        return category;
    }

    public void setCategory(L3NetworkCategory category) {
        this.category = category;
    }

    public Integer getIpVersion() {
        return ipVersion;
    }

    public void setIpVersion(Integer ipVersion) {
        this.ipVersion = ipVersion;
    }

    public Boolean getEnableIPAM() {
        return enableIPAM;
    }

    public void setEnableIPAM(Boolean enableIPAM) {
        this.enableIPAM = enableIPAM;
    }
}
