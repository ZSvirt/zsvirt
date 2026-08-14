package org.zstack.header.baremetal.chassis;

import org.zstack.header.baremetal.pxeserver.BaremetalPxeServerVO;
import org.zstack.header.cluster.ClusterEO;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.*;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.zone.ZoneEO;
import org.zstack.header.zone.ZoneVO;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by GuoYi on 4/26/17.
 */
@Entity
@Table
@AutoDeleteTag
@BaseResource
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = ZoneVO.class, myField = "zoneUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = ClusterVO.class, myField = "clusterUuid", targetField = "uuid"),
        },

        friends = {
                @EntityGraph.Neighbour(type = BaremetalHardwareInfoVO.class, myField = "uuid", targetField = "chassisUuid"),
                @EntityGraph.Neighbour(type = BaremetalPxeServerVO.class, myField = "pxeServerUuid", targetField = "uuid")
        }
)
public class BaremetalChassisVO extends ResourceVO implements ToInventory {
    @Column
    private String name;

    @Column
    private String description;

    @Column
    @ForeignKey(parentEntityClass = ZoneEO.class, onDeleteAction = ReferenceOption.RESTRICT)
    private String zoneUuid;

    @Column
    @ForeignKey(parentEntityClass = ClusterEO.class, onDeleteAction = ReferenceOption.RESTRICT)
    private String clusterUuid;

    @Column
    @ForeignKey(parentEntityClass = BaremetalPxeServerVO.class, onDeleteAction = ForeignKey.ReferenceOption.SET_NULL)
    private String pxeServerUuid;

    @Column
    private String ipmiAddress;

    @Column
    private Integer ipmiPort;

    @Column
    private String ipmiUsername;

    @Column
    private String ipmiPassword;

    @Column
    @Enumerated(EnumType.STRING)
    private BaremetalChassisState state;

    @Column
    @Enumerated(EnumType.STRING)
    private BaremetalChassisStatus status;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "chassisUuid", insertable = false, updatable = false)
    @NoView
    private Set<BaremetalHardwareInfoVO> hardwareInfos = new HashSet<>();

    public Set<BaremetalHardwareInfoVO> getHardwareInfos() {
        return hardwareInfos;
    }

    public void setHardwareInfos(Set<BaremetalHardwareInfoVO> hardwareInfos) {
        this.hardwareInfos = hardwareInfos;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public String getPxeServerUuid() {
        return pxeServerUuid;
    }

    public void setPxeServerUuid(String pxeServerUuid) {
        this.pxeServerUuid = pxeServerUuid;
    }

    public String getIpmiAddress() {
        return ipmiAddress;
    }

    public void setIpmiAddress(String ipmiAddress) {
        this.ipmiAddress = ipmiAddress;
    }

    public Integer getIpmiPort() {
        return ipmiPort;
    }

    public void setIpmiPort(Integer ipmiPort) {
        this.ipmiPort = ipmiPort;
    }

    public String getIpmiUsername() {
        return ipmiUsername;
    }

    public void setIpmiUsername(String ipmiUsername) {
        this.ipmiUsername = ipmiUsername;
    }

    public String getIpmiPassword() {
        return ipmiPassword;
    }

    public void setIpmiPassword(String ipmiPassword) {
        this.ipmiPassword = ipmiPassword;
    }

    public BaremetalChassisState getState() {
        return state;
    }

    public void setState(BaremetalChassisState state) {
        this.state = state;
    }

    public BaremetalChassisStatus getStatus() {
        return status;
    }

    public void setStatus(BaremetalChassisStatus status) {
        this.status = status;
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
