package org.zstack.header.baremetal.instance;

import org.zstack.header.baremetal.chassis.BaremetalChassisVO;
import org.zstack.header.baremetal.network.BaremetalNicVO;
import org.zstack.header.baremetal.preconfiguration.PreconfigurationTemplateVO;
import org.zstack.header.baremetal.pxeserver.BaremetalPxeServerVO;
import org.zstack.header.cluster.ClusterEO;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.core.encrypt.ENCRYPTParam;
import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.image.ImageEO;
import org.zstack.header.image.ImageVO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.Index;
import org.zstack.header.vo.*;
import org.zstack.header.zone.ZoneEO;
import org.zstack.header.zone.ZoneVO;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by GuoYi on 7/4/18.
 */
@Entity
@Table
@AutoDeleteTag
@BaseResource
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = ZoneVO.class, myField = "zoneUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = ClusterVO.class, myField = "clusterUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = BaremetalChassisVO.class, myField = "chassisUuid", targetField = "uuid"),
        },

        friends = {
                @EntityGraph.Neighbour(type = ImageVO.class, myField = "imageUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = BaremetalNicVO.class, myField = "uuid", targetField = "baremetalInstanceUuid"),
                @EntityGraph.Neighbour(type = BaremetalPxeServerVO.class, myField = "pxeServerUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = PreconfigurationTemplateVO.class, myField = "templateUuid", targetField = "uuid"),
        }
)
public class BaremetalInstanceVO extends ResourceVO implements ToInventory, OwnedByAccount {
    @Column
    @Index(length = 128)
    private String name;

    @Column
    private String description;

    @Column
    private Long internalId;

    @Column
    @ForeignKey(parentEntityClass = ZoneEO.class, onDeleteAction = ForeignKey.ReferenceOption.SET_NULL)
    private String zoneUuid;

    @Column
    @ForeignKey(parentEntityClass = ClusterEO.class, onDeleteAction = ForeignKey.ReferenceOption.SET_NULL)
    private String clusterUuid;

    @Column
    @ForeignKey(parentEntityClass = BaremetalPxeServerVO.class, onDeleteAction = ForeignKey.ReferenceOption.SET_NULL)
    private String pxeServerUuid;

    @Column
    @ForeignKey(parentEntityClass = BaremetalChassisVO.class, onDeleteAction = ForeignKey.ReferenceOption.RESTRICT)
    private String chassisUuid;

    @Column
    @ForeignKey(parentEntityClass = ImageEO.class, onDeleteAction = ForeignKey.ReferenceOption.SET_NULL)
    private String imageUuid;

    @Column
    @ForeignKey(parentEntityClass = PreconfigurationTemplateVO.class, onDeleteAction = ForeignKey.ReferenceOption.SET_NULL)
    private String templateUuid;

    @Column
    private String platform;

    // to take over pre-existing baremetal instances
    @Column
    private String managementIp;

    @Column
    private String username;

    @Column
    @ENCRYPTParam
    private String password;

    @Column
    private Integer port;

    @Column
    @Enumerated(EnumType.STRING)
    private BaremetalInstanceState state;

    @Column
    @Enumerated(EnumType.STRING)
    private BaremetalInstanceStatus status;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "baremetalInstanceUuid", insertable = false, updatable = false)
    @NoView
    private Set<BaremetalNicVO> bmNics = new HashSet<>();

    @Transient
    private String accountUuid;

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

    public Long getInternalId() {
        return internalId;
    }

    public void setInternalId(Long internalId) {
        this.internalId = internalId;
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

    public String getChassisUuid() {
        return chassisUuid;
    }

    public void setChassisUuid(String chassisUuid) {
        this.chassisUuid = chassisUuid;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public String getTemplateUuid() {
        return templateUuid;
    }

    public void setTemplateUuid(String templateUuid) {
        this.templateUuid = templateUuid;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getManagementIp() {
        return managementIp;
    }

    public void setManagementIp(String managementIp) {
        this.managementIp = managementIp;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public BaremetalInstanceState getState() {
        return state;
    }

    public void setState(BaremetalInstanceState state) {
        this.state = state;
    }

    public BaremetalInstanceStatus getStatus() {
        return status;
    }

    public void setStatus(BaremetalInstanceStatus status) {
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

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public Set<BaremetalNicVO> getBmNics() {
        return bmNics;
    }

    public void setBmNics(Set<BaremetalNicVO> bmNics) {
        this.bmNics = bmNics;
    }

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public BaremetalInstanceVO() {
    }

    public BaremetalInstanceVO(BaremetalInstanceVO other) {
        this.uuid = other.uuid;
        this.name = other.name;
        this.description = other.description;
        this.internalId = other.internalId;
        this.zoneUuid = other.zoneUuid;
        this.clusterUuid = other.clusterUuid;
        this.pxeServerUuid = other.pxeServerUuid;
        this.chassisUuid = other.chassisUuid;
        this.imageUuid = other.imageUuid;
        this.templateUuid = other.templateUuid;
        this.platform = other.platform;
        this.managementIp = other.managementIp;
        this.username = other.username;
        this.password = other.password;
        this.port = other.port;
        this.state = other.state;
        this.status = other.status;
        this.createDate = other.createDate;
        this.lastOpDate = other.lastOpDate;

        this.bmNics = other.bmNics;
        this.accountUuid = other.accountUuid;
    }
}
