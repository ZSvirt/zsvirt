package org.zstack.header.host;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@PythonClassInventory
@Inventory(mappingVOClass = HostNetworkBondingServiceRefVO.class)
public class HostNetworkBondingServiceRefInventory implements Serializable {
    @APINoSee
    private long id;
    private String bondingUuid;
    private Integer vlanId;
    private HostNetworkInterfaceServiceType serviceType;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getBondingUuid() {
        return bondingUuid;
    }

    public void setBondingUuid(String bondingUuid) {
        this.bondingUuid = bondingUuid;
    }

    public Integer getVlanId() {
        return vlanId;
    }

    public void setVlanId(Integer vlanId) {
        this.vlanId = vlanId;
    }

    public HostNetworkInterfaceServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(HostNetworkInterfaceServiceType serviceType) {
        this.serviceType = serviceType;
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

    public HostNetworkBondingServiceRefInventory() {

    }

    public HostNetworkBondingServiceRefInventory(HostNetworkBondingServiceRefVO vo) {
        this.bondingUuid = vo.getBondingUuid();
        this.vlanId = vo.getVlanId();
        this.serviceType = vo.getServiceType();
        this.createDate = vo.getCreateDate();
        this.lastOpDate = vo.getLastOpDate();
    }

    public static HostNetworkBondingServiceRefInventory valueOf(HostNetworkBondingServiceRefVO vo) {
        return new HostNetworkBondingServiceRefInventory(vo);
    }

    public static List<HostNetworkBondingServiceRefInventory> valueOf(Collection<HostNetworkBondingServiceRefVO> vos) {
        return vos.stream().map(HostNetworkBondingServiceRefInventory::valueOf).collect(Collectors.toList());
    }
}
