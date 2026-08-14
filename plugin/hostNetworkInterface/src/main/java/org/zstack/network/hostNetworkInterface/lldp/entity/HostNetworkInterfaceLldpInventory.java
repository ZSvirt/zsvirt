package org.zstack.network.hostNetworkInterface.lldp.entity;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.*;
import org.zstack.header.search.Inventory;

import javax.persistence.JoinColumn;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@PythonClassInventory
@Inventory(mappingVOClass = HostNetworkInterfaceLldpVO.class)
public class HostNetworkInterfaceLldpInventory implements Serializable {
    private String uuid;
    private String interfaceUuid;
    private String mode;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    @Queryable(mappingClass = HostNetworkInterfaceLldpRefInventory.class,
            joinColumn = @JoinColumn(name = "lldpUuid", referencedColumnName = "neighborDevice"))
    private HostNetworkInterfaceLldpRefInventory neighborDevice;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getInterfaceUuid() {
        return interfaceUuid;
    }

    public void setInterfaceUuid(String interfaceUuid) {
        this.interfaceUuid = interfaceUuid;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public HostNetworkInterfaceLldpRefInventory getNeighborDevice() {
        return neighborDevice;
    }

    public void setNeighborDevice(HostNetworkInterfaceLldpRefInventory neighborDevice) {
        this.neighborDevice = neighborDevice;
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

    public HostNetworkInterfaceLldpInventory() {

    }

    public HostNetworkInterfaceLldpInventory(HostNetworkInterfaceLldpVO vo) {
        this.uuid = vo.getUuid();
        this.interfaceUuid = vo.getInterfaceUuid();
        this.mode = vo.getMode();
        this.createDate = vo.getCreateDate();
        this.lastOpDate = vo.getLastOpDate();
        this.neighborDevice = vo.getNeighborDevice() != null ? HostNetworkInterfaceLldpRefInventory.valueOf(vo.getNeighborDevice()) : null;
    }

    public static HostNetworkInterfaceLldpInventory valueOf(HostNetworkInterfaceLldpVO vo) {
        return new HostNetworkInterfaceLldpInventory(vo);
    }

    public static List<HostNetworkInterfaceLldpInventory> valueOf(Collection<HostNetworkInterfaceLldpVO> vos) {
        return vos.stream().map(HostNetworkInterfaceLldpInventory::valueOf).collect(Collectors.toList());
    }
}
