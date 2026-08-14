package org.zstack.header.vm;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = VmDnsVO.class, collectionValueOfMethod = "valueOf1")
public class VmDnsInventory {
    private String vmInstanceUuid;
    private String vmNicUuid;
    private String dns;
    private Integer ipVersion;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }

    public String getDns() {
        return dns;
    }

    public void setDns(String dns) {
        this.dns = dns;
    }

    public Integer getIpVersion() {
        return ipVersion;
    }

    public void setIpVersion(Integer ipVersion) {
        this.ipVersion = ipVersion;
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

    public VmDnsInventory() {
    }

    public VmDnsInventory(VmDnsVO vo) {
        this.setVmInstanceUuid(vo.getVmInstanceUuid());
        this.setVmNicUuid(vo.getVmNicUuid());
        this.setDns(vo.getDns());
        this.setIpVersion(vo.getIpVersion());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public static VmDnsInventory valueOf(VmDnsVO vo) {
        return new VmDnsInventory(vo);
    }

    public static List<VmDnsInventory> valueOf1(Collection<VmDnsVO> vos) {
        return vos.stream().map(VmDnsInventory::new).collect(Collectors.toList());
    }
}
