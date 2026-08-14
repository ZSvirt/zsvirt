package org.zstack.header.baremetal.pxeserver;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.log.NoLogging;
import org.zstack.header.query.*;
import org.zstack.header.search.Inventory;
import org.zstack.header.zone.ZoneInventory;

import javax.persistence.JoinColumn;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by GuoYi on 17/4/20.
 */
@Inventory(mappingVOClass = BaremetalPxeServerVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "zone", inventoryClass = ZoneInventory.class,
                foreignKey = "zoneUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "clusterRef", inventoryClass = BaremetalPxeServerClusterRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "pxeServerUuid", hidden = true),
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(alias = "cluster", expandedField = "clusterRef.cluster")
})
public class BaremetalPxeServerInventory implements Serializable {
    private String uuid;
    private String zoneUuid;
    private String name;
    private String description;
    private String hostname;
    private String sshUsername;
    @NoLogging
    private String sshPassword;
    private Integer sshPort;
    private String storagePath;
    private String dhcpInterface;
    private String dhcpInterfaceAddress;
    private String dhcpRangeBegin;
    private String dhcpRangeEnd;
    private String dhcpRangeNetmask;
    private String state;
    private String status;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private Long totalCapacity;
    private Long availableCapacity;

    @Queryable(mappingClass = BaremetalPxeServerClusterRefInventory.class,
            joinColumn = @JoinColumn(name = "pxeServerUuid", referencedColumnName = "clusterUuid"))
    private List<String> attachedClusterUuids;

    public BaremetalPxeServerInventory(){

    }

    public BaremetalPxeServerInventory(BaremetalPxeServerInventory other) {
        uuid = other.getUuid();
        zoneUuid = other.getZoneUuid();
        name = other.getName();
        description = other.getDescription();
        dhcpInterface = other.getDhcpInterface();
        dhcpInterfaceAddress = other.getDhcpInterfaceAddress();
        dhcpRangeBegin = other.getDhcpRangeBegin();
        dhcpRangeEnd = other.getDhcpRangeEnd();
        dhcpRangeNetmask = other.getDhcpRangeNetmask();
        status = other.status;
        createDate = other.getCreateDate();
        lastOpDate = other.getLastOpDate();

        attachedClusterUuids = other.getAttachedClusterUuids();
        totalCapacity = other.getTotalCapacity();
        availableCapacity = other.getAvailableCapacity();
    }

    public static BaremetalPxeServerInventory valueOf(BaremetalPxeServerVO vo) {
        BaremetalPxeServerInventory inv = new BaremetalPxeServerInventory();
        inv.setUuid(vo.getUuid());
        inv.setZoneUuid(vo.getZoneUuid());
        inv.setName(vo.getName());
        inv.setDescription(vo.getDescription());
        inv.setHostname(vo.getHostname());
        inv.setSshUsername(vo.getSshUsername());
        inv.setSshPassword(vo.getSshPassword());
        inv.setSshPort(vo.getSshPort());
        inv.setStoragePath(vo.getStoragePath());
        inv.setDhcpInterface(vo.getDhcpInterface());
        inv.setDhcpInterfaceAddress(vo.getDhcpInterfaceAddress());
        inv.setDhcpRangeBegin(vo.getDhcpRangeBegin());
        inv.setDhcpRangeEnd(vo.getDhcpRangeEnd());
        inv.setDhcpRangeNetmask(vo.getDhcpRangeNetmask());
        inv.setState(vo.getState().toString());
        inv.setStatus(vo.getStatus().toString());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setTotalCapacity(vo.getTotalCapacity());
        inv.setAvailableCapacity(vo.getAvailableCapacity());

        inv.attachedClusterUuids = new ArrayList<>(vo.getAttachedClusterRefs().size());
        for (BaremetalPxeServerClusterRefVO ref : vo.getAttachedClusterRefs()) {
            inv.attachedClusterUuids.add(ref.getClusterUuid());
        }
        return inv;
    }

    public static List<BaremetalPxeServerInventory> valueOf(Collection<BaremetalPxeServerVO> vos) {
        List<BaremetalPxeServerInventory> inventories = new ArrayList<>();
        for (BaremetalPxeServerVO vo: vos) {
            inventories.add(valueOf(vo));
        }
        return inventories;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
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

    public Integer getSshPort() {
        return sshPort;
    }

    public void setSshPort(Integer sshPort) {
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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

    public Long getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(Long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public Long getAvailableCapacity() {
        return availableCapacity;
    }

    public void setAvailableCapacity(Long availableCapacity) {
        this.availableCapacity = availableCapacity;
    }

    public List<String> getAttachedClusterUuids() {
        return attachedClusterUuids;
    }

    public void setAttachedClusterUuids(List<String> attachedClusterUuids) {
        this.attachedClusterUuids = attachedClusterUuids;
    }
}
