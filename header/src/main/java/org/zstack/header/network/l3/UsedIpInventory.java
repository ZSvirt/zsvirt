package org.zstack.header.network.l3;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vm.VmNicVO;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = UsedIpVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "l3Network", inventoryClass = L3NetworkInventory.class,
                foreignKey = "l3NetworkUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "vmNic", inventoryClass = VmNicInventory.class,
                foreignKey = "vmNicUuid", expandedInventoryKey = "uuid"),
})
public class UsedIpInventory implements Serializable {
    private String uuid;
    private String ipRangeUuid;
    private String l3NetworkUuid;
    private Integer ipVersion;
    private String ip;
    private String netmask;
    private String gateway;
    private String usedFor;
    @APINoSee
    private String metaData;
    private Long ipInLong;
    @APINoSee
    private byte[] ipInBinary;
    private String vmNicUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public UsedIpInventory() {
    }

    public UsedIpInventory(UsedIpVO vo) {
        this.setUuid(vo.getUuid());
        this.setIpVersion(vo.getIpVersion());
        this.setIp(vo.getIp());
        this.setIpInLong(vo.getIpInLong());
        this.setIpInBinary(vo.getIpInBinary());
        this.setIpRangeUuid(vo.getIpRangeUuid());
        this.setL3NetworkUuid(vo.getL3NetworkUuid());
        this.setGateway(vo.getGateway());
        this.setNetmask(vo.getNetmask());
        this.setUsedFor(vo.getUsedFor());
        this.setVmNicUuid(vo.getVmNicUuid());
        this.setMetaData(vo.getMetaData());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public static UsedIpInventory valueOf(UsedIpVO vo) {
        return new UsedIpInventory(vo);
    }

    public static List<UsedIpInventory> valueOf(Collection<UsedIpVO> vos) {
        List<UsedIpInventory> invs = new ArrayList<UsedIpInventory>(vos.size());
        vos = vos.stream().sorted(Comparator.comparingInt(UsedIpVO::getIpVersion)).collect(Collectors.toList());
        for (UsedIpVO vo : vos) {
            invs.add(UsedIpInventory.valueOf(vo));
        }
        return invs;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getIpRangeUuid() {
        return ipRangeUuid;
    }

    public void setIpRangeUuid(String ipRangeUuid) {
        this.ipRangeUuid = ipRangeUuid;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Long getIpInLong() {
        return ipInLong;
    }

    public void setIpInLong(long ipInLong) {
        this.ipInLong = ipInLong;
    }

    public byte[] getIpInBinary() {
        return ipInBinary;
    }

    public void setIpInBinary(byte[] ipInBinary) {
        this.ipInBinary = ipInBinary;
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

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
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

    public String getUsedFor() {
        return usedFor;
    }

    public void setUsedFor(String usedFor) {
        this.usedFor = usedFor;
    }

    public String getMetaData() {
        return metaData;
    }

    public void setMetaData(String metaData) {
        this.metaData = metaData;
    }

    public Integer getIpVersion() {
        return ipVersion;
    }

    public void setIpVersion(Integer ipVersion) {
        this.ipVersion = ipVersion;
    }

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }
}
