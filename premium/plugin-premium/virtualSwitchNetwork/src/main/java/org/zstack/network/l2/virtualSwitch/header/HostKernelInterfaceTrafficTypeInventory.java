package org.zstack.network.l2.virtualSwitch.header;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;

@PythonClassInventory
@Inventory(mappingVOClass = HostKernelInterfaceTrafficTypeVO.class, collectionValueOfMethod = "valueOf1")
@ExpandedQueries({
    @ExpandedQuery(expandedField = "hostKernelInterface", inventoryClass = HostKernelInterfaceInventory.class,
            foreignKey = "hostKernelInterfaceUuid", expandedInventoryKey = "uuid"),
})
public class HostKernelInterfaceTrafficTypeInventory implements Serializable {
    @APINoSee
    private Long id;
    private String hostKernelInterfaceUuid;
    private HostKernelInterfaceTrafficType trafficType;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHostKernelInterfaceUuid() {
        return hostKernelInterfaceUuid;
    }

    public void setHostKernelInterfaceUuid(String hostKernelInterfaceUuid) {
        this.hostKernelInterfaceUuid = hostKernelInterfaceUuid;
    }

    public HostKernelInterfaceTrafficType getTrafficType() {
        return trafficType;
    }

    public void setTrafficType(HostKernelInterfaceTrafficType trafficType) {
        this.trafficType = trafficType;
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

    public HostKernelInterfaceTrafficTypeInventory() {

    }

    public HostKernelInterfaceTrafficTypeInventory(HostKernelInterfaceTrafficTypeVO vo) {
        this.setHostKernelInterfaceUuid(vo.getHostKernelInterfaceUuid());
        this.setTrafficType(vo.getTrafficType());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public static HostKernelInterfaceTrafficTypeInventory valueOf(HostKernelInterfaceTrafficTypeVO vo) {
        return new HostKernelInterfaceTrafficTypeInventory(vo);
    }

    public static List<HostKernelInterfaceTrafficTypeInventory> valueOf1(Collection<HostKernelInterfaceTrafficTypeVO> vos) {
        List<HostKernelInterfaceTrafficTypeInventory> invs = new ArrayList<>(vos.size());
        for (HostKernelInterfaceTrafficTypeVO vo : vos) {
            invs.add(new HostKernelInterfaceTrafficTypeInventory(vo));
        }
        return invs;
    }

}
