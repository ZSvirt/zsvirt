package org.zstack.header.vm;

import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.UsedIpInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;
import org.zstack.utils.network.IPv6Constants;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = VmNicVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "vmInstance", inventoryClass = VmInstanceInventory.class,
                foreignKey = "vmInstanceUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "l3Network", inventoryClass = L3NetworkInventory.class,
                foreignKey = "l3NetworkUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "usedIp", inventoryClass = UsedIpInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "vmNicUuid"),
})
public class VmNicInventory implements Serializable {
    private String uuid;
    private String vmInstanceUuid;
    @APINoSee
    private String usedIpUuid;
    private String l3NetworkUuid;
    private String ip;
    private String mac;
    private String hypervisorType;
    private String netmask;
    private String gateway;
    private String metaData;
    @Deprecated
    @APINoSee
    private Integer ipVersion;
    private String driverType;
    private List<UsedIpInventory> usedIps;
    private String internalName;
    private Integer deviceId;
    private String type;
    private String state;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public VmNicInventory() {
    }

    public VmNicInventory(VmNicVO vo) {
        this.setUuid(vo.getUuid());
        this.setVmInstanceUuid(vo.getVmInstanceUuid());
        this.setUsedIpUuid(vo.getUsedIpUuid());
        this.setL3NetworkUuid(vo.getL3NetworkUuid());
        this.setInternalName(vo.getInternalName());
        this.setIp(vo.getIp());
        this.setMac(vo.getMac());
        this.setHypervisorType(vo.getHypervisorType());
        this.setDeviceId(vo.getDeviceId());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setMetaData(vo.getMetaData());
        this.setNetmask(vo.getNetmask());
        this.setGateway(vo.getGateway());
        this.setUsedIps(UsedIpInventory.valueOf(vo.getUsedIps()));
        this.setDriverType(vo.getDriverType());
        this.setType(vo.getType());
        this.setState(vo.getState().toString());
    }

    public static VmNicInventory valueOf(VmNicVO vo) {
        return new VmNicInventory(vo);
    }

    public static List<VmNicInventory> valueOf(Collection<VmNicVO> vos) {
        List<VmNicInventory> invs = new ArrayList<VmNicInventory>(vos.size());
        vos = vos.stream().sorted(Comparator.comparingInt(VmNicVO::getDeviceId)).collect(Collectors.toList());
        for (VmNicVO vo : vos) {
            invs.add(VmNicInventory.valueOf(vo));
        }
        return invs;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getUsedIpUuid() {
        return usedIpUuid;
    }

    public void setUsedIpUuid(String usedIpUuid) {
        this.usedIpUuid = usedIpUuid;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getHypervisorType() {
        return hypervisorType;
    }

    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
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

    public String getMetaData() {
        return metaData;
    }

    public void setMetaData(String metaData) {
        this.metaData = metaData;
    }

    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getInternalName() {
        return internalName;
    }

    public void setInternalName(String internalName) {
        this.internalName = internalName;
    }

    @Deprecated
    public Integer getIpVersion() {
        return ipVersion;
    }

    @Deprecated
    public void setIpVersion(Integer ipVersion) {
        this.ipVersion = ipVersion;
    }

    public List<UsedIpInventory> getUsedIps() {
        return usedIps;
    }

    public void setUsedIps(List<UsedIpInventory> usedIps) {
        this.usedIps = usedIps;
    }

    public String getDriverType() {
        return driverType;
    }

    public void setDriverType(String driverType) {
        this.driverType = driverType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isIpv6OnlyNic() {
        return this.usedIps.size() == 1 && this.usedIps.get(0).getIpVersion() == IPv6Constants.IPv6;
    }

    public boolean isIpv4OnlyNic() {
        return this.usedIps.size() == 1 && this.usedIps.get(0).getIpVersion() == IPv6Constants.IPv4;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
