package org.zstack.network.hostNetworkInterface.lldp;

public class LldpInfoStruct {
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

}
