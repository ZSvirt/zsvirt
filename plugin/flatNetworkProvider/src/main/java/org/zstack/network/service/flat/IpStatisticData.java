package org.zstack.network.service.flat;

import java.sql.Timestamp;
import java.util.List;

/**
 * Created by Qi Le on 2019/9/10
 */
public class IpStatisticData {
    private String ip;
    private String vipUuid;
    private String vipName;
    private String vmInstanceUuid;
    private String vmInstanceName;
    private String vmInstanceType;
    private String applianceVmOwnerUuid;
    private List<String> vmDefaultIp;
    private List<String> resourceTypes;
    private String state;
    private String useFor;
    private Timestamp createDate;
    private String ownerName;
    private String resourceOwnerUuid;
    private String usedIpUuid;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
    }

    public String getVipName() {
        return vipName;
    }

    public void setVipName(String vipName) {
        this.vipName = vipName;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getVmInstanceName() {
        return vmInstanceName;
    }

    public void setVmInstanceName(String vmInstanceName) {
        this.vmInstanceName = vmInstanceName;
    }

    public String getVmInstanceType() {
        return vmInstanceType;
    }

    public void setVmInstanceType(String vmInstanceType) {
        this.vmInstanceType = vmInstanceType;
    }

    public List<String> getResourceTypes() {
        return resourceTypes;
    }

    public void setResourceTypes(List<String> resourceTypes) {
        this.resourceTypes = resourceTypes;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getUseFor() {
        return useFor;
    }

    public void setUseFor(String useFor) {
        this.useFor = useFor;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public List<String> getVmDefaultIp() {
        return vmDefaultIp;
    }

    public void setVmDefaultIp(List<String> vmDefaultIp) {
        this.vmDefaultIp = vmDefaultIp;
    }

    public String getApplianceVmOwnerUuid() {
        return applianceVmOwnerUuid;
    }

    public void setApplianceVmOwnerUuid(String vmInstanceParentUuid) {
        this.applianceVmOwnerUuid = vmInstanceParentUuid;
    }

    public String getResourceOwnerUuid() {
        return resourceOwnerUuid;
    }

    public void setResourceOwnerUuid(String resourceOwnerUuid) {
        this.resourceOwnerUuid = resourceOwnerUuid;
    }

    public String getUsedIpUuid() {
        return usedIpUuid;
    }

    public void setUsedIpUuid(String usedIpUuid) {
        this.usedIpUuid = usedIpUuid;
    }
}
