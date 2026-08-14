package org.zstack.header.baremetal.pxeserver;

import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.*;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.zone.ZoneEO;
import org.zstack.header.zone.ZoneVO;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by GuoYi on 4/20/17.
 */

@Entity
@Table
@AutoDeleteTag
@BaseResource
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = ZoneVO.class, myField = "zoneUuid", targetField = "uuid")
        },
        friends = {
                @EntityGraph.Neighbour(type = BaremetalPxeServerClusterRefVO.class, myField = "uuid", targetField = "pxeServerUuid")
        }
)
public class BaremetalPxeServerVO extends ResourceVO implements ToInventory {
    @Column
    @ForeignKey(parentEntityClass = ZoneEO.class, onDeleteAction = ForeignKey.ReferenceOption.RESTRICT)
    private String zoneUuid;

    @Column
    private String name;

    @Column
    private String description;

    @Column
    private String hostname;

    @Column
    private String sshUsername;

    @Column
    private String sshPassword;

    @Column
    private int sshPort = 22;

    @Column
    private String storagePath;

    @Column
    private String dhcpInterface;

    @Column
    private String dhcpInterfaceAddress;

    @Column
    private String dhcpRangeBegin;

    @Column
    private String dhcpRangeEnd;

    @Column
    private String dhcpRangeNetmask;

    @Column
    private long totalCapacity;

    @Column
    private long availableCapacity;

    @Column
    @Enumerated(EnumType.STRING)
    private BaremetalPxeServerState state;

    @Column
    @Enumerated(EnumType.STRING)
    private BaremetalPxeServerStatus status;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "pxeServerUuid", insertable = false, updatable = false)
    @NoView
    private Set<BaremetalPxeServerClusterRefVO> attachedClusterRefs = new HashSet<>();

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
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

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getSshUsername() {
        return sshUsername;
    }

    public void setSshUsername(String sshUsername) {
        this.sshUsername = sshUsername;
    }

    public String getSshPassword() {
        return sshPassword;
    }

    public void setSshPassword(String sshPassword) {
        this.sshPassword = sshPassword;
    }

    public int getSshPort() {
        return sshPort;
    }

    public void setSshPort(int sshPort) {
        this.sshPort = sshPort;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getDhcpInterface() {
        return dhcpInterface;
    }

    public void setDhcpInterface(String dhcpInterface) {
        this.dhcpInterface = dhcpInterface;
    }

    public String getDhcpInterfaceAddress() {
        return dhcpInterfaceAddress;
    }

    public void setDhcpInterfaceAddress(String dhcpInterfaceAddress) {
        this.dhcpInterfaceAddress = dhcpInterfaceAddress;
    }

    public String getDhcpRangeBegin() {
        return dhcpRangeBegin;
    }

    public void setDhcpRangeBegin(String dhcpRangeBegin) {
        this.dhcpRangeBegin = dhcpRangeBegin;
    }

    public String getDhcpRangeEnd() {
        return dhcpRangeEnd;
    }

    public void setDhcpRangeEnd(String dhcpRangeEnd) {
        this.dhcpRangeEnd = dhcpRangeEnd;
    }

    public String getDhcpRangeNetmask() {
        return dhcpRangeNetmask;
    }

    public void setDhcpRangeNetmask(String dhcpRangeNetmask) {
        this.dhcpRangeNetmask = dhcpRangeNetmask;
    }

    public long getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public long getAvailableCapacity() {
        return availableCapacity;
    }

    public void setAvailableCapacity(long availableCapacity) {
        this.availableCapacity = availableCapacity;
    }

    public BaremetalPxeServerState getState() {
        return state;
    }

    public void setState(BaremetalPxeServerState state) {
        this.state = state;
    }

    public BaremetalPxeServerStatus getStatus() {
        return status;
    }

    public void setStatus(BaremetalPxeServerStatus status) {
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

    public Set<BaremetalPxeServerClusterRefVO> getAttachedClusterRefs() {
        return attachedClusterRefs;
    }

    public void setAttachedClusterRefs(Set<BaremetalPxeServerClusterRefVO> attachedClusterRefs) {
        this.attachedClusterRefs = attachedClusterRefs;
    }
}
