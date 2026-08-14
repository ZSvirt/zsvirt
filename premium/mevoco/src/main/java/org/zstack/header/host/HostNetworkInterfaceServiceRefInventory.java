package org.zstack.header.host;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;

import javax.persistence.Column;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@PythonClassInventory
@Inventory(mappingVOClass = HostNetworkInterfaceServiceRefVO.class)
public class HostNetworkInterfaceServiceRefInventory implements Serializable {
    @APINoSee
    private long id;
    private String interfaceUuid;
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

    public String getInterfaceUuid() {
        return interfaceUuid;
    }

    public void setInterfaceUuid(String interfaceUuid) {
        this.interfaceUuid = interfaceUuid;
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

    public HostNetworkInterfaceServiceRefInventory() {

    }

    public HostNetworkInterfaceServiceRefInventory(HostNetworkInterfaceServiceRefVO vo) {
        this.interfaceUuid = vo.getInterfaceUuid();
        this.vlanId = vo.getVlanId();
        this.serviceType = vo.getServiceType();
        this.createDate = vo.getCreateDate();
        this.lastOpDate = vo.getLastOpDate();
    }

    public static HostNetworkInterfaceServiceRefInventory valueOf(HostNetworkInterfaceServiceRefVO vo) {
        return new HostNetworkInterfaceServiceRefInventory(vo);
    }

    public static List<HostNetworkInterfaceServiceRefInventory> valueOf(Collection<HostNetworkInterfaceServiceRefVO> vos) {
        return vos.stream().map(HostNetworkInterfaceServiceRefInventory::valueOf).collect(Collectors.toList());
    }
}
