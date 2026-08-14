package org.zstack.network.hostNetworkInterface.lldp.entity;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@PythonClassInventory
@Inventory(mappingVOClass = HostNetworkInterfaceLldpRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "neighborDevice", inventoryClass = HostNetworkInterfaceLldpInventory.class,
                foreignKey = "lldpUuid", expandedInventoryKey = "uuid"),
})
public class HostNetworkInterfaceLldpRefInventory implements Serializable {
    private String lldpUuid;
    private String chassisId;
    private Integer timeToLive;
    private String managementAddress;
    private String systemName;
    private String systemDescription;
    private String systemCapabilities;
    private String portId;
    private String portDescription;
    private Integer vlanId;
    private Long aggregationPortId;
    private Integer mtu;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public String getLldpUuid() {
        return lldpUuid;
    }

    public void setLldpUuid(String lldpUuid) {
        this.lldpUuid = lldpUuid;
    }

    public String getChassisId() {
        return chassisId;
    }

    public void setChassisId(String chassisId) {
        this.chassisId = chassisId;
    }

    public Integer getTimeToLive() {
        return timeToLive;
    }

    public void setTimeToLive(Integer timeToLive) {
        this.timeToLive = timeToLive;
    }

    public String getManagementAddress() {
        return managementAddress;
    }

    public void setManagementAddress(String managementAddress) {
        this.managementAddress = managementAddress;
    }

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public String getSystemDescription() {
        return systemDescription;
    }

    public void setSystemDescription(String systemDescription) {
        this.systemDescription = systemDescription;
    }

    public String getSystemCapabilities() {
        return systemCapabilities;
    }

    public void setSystemCapabilities(String systemCapabilities) {
        this.systemCapabilities = systemCapabilities;
    }

    public String getPortId() {
        return portId;
    }

    public void setPortId(String portId) {
        this.portId = portId;
    }

    public String getPortDescription() {
        return portDescription;
    }

    public void setPortDescription(String portDescription) {
        this.portDescription = portDescription;
    }

    public Integer getVlanId() {
        return vlanId;
    }

    public void setVlanId(Integer vlanId) {
        this.vlanId = vlanId;
    }

    public Long getAggregationPortId() {
        return aggregationPortId;
    }

    public void setAggregationPortId(Long aggregationPortId) {
        this.aggregationPortId = aggregationPortId;
    }

    public Integer getMtu() {
        return mtu;
    }

    public void setMtu(Integer mtu) {
        this.mtu = mtu;
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

    public HostNetworkInterfaceLldpRefInventory() {

    }

    public HostNetworkInterfaceLldpRefInventory(HostNetworkInterfaceLldpRefVO vo) {
        this.lldpUuid = vo.getLldpUuid();
        this.chassisId = vo.getChassisId();
        this.timeToLive = vo.getTimeToLive();
        this.managementAddress = vo.getManagementAddress();
        this.systemName = vo.getSystemName();
        this.systemDescription = vo.getSystemDescription();
        this.systemCapabilities = vo.getSystemCapabilities();
        this.portId = vo.getPortId();
        this.portDescription = vo.getPortDescription();
        this.vlanId = vo.getVlanId();
        this.aggregationPortId = vo.getAggregationPortId();
        this.mtu = vo.getMtu();
        this.createDate = vo.getCreateDate();
        this.lastOpDate = vo.getLastOpDate();
    }

    public static HostNetworkInterfaceLldpRefInventory valueOf(HostNetworkInterfaceLldpRefVO vo) {
        return new HostNetworkInterfaceLldpRefInventory(vo);
    }

    public static List<HostNetworkInterfaceLldpRefInventory> valueOf(Collection<HostNetworkInterfaceLldpRefVO> vos) {
        return vos.stream().map(HostNetworkInterfaceLldpRefInventory::valueOf).collect(Collectors.toList());
    }
}
