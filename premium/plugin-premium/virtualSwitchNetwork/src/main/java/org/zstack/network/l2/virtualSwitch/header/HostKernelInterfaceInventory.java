package org.zstack.network.l2.virtualSwitch.header;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.persistence.JoinColumn;


import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.DocUtils;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.Queryable;
import org.zstack.header.search.Inventory;

@PythonClassInventory
@Inventory(mappingVOClass = HostKernelInterfaceVO.class, collectionValueOfMethod = "valueOf1")
@ExpandedQueries({
        @ExpandedQuery(expandedField = "portGroup", inventoryClass = PortGroupInventory.class,
                foreignKey = "l3NetworkUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "usedIp", inventoryClass = HostKernelInterfaceUsedIpInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "hostKernelInterfaceUuid", hidden = true),
        @ExpandedQuery(expandedField = "trafficType", inventoryClass = HostKernelInterfaceTrafficTypeInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "hostKernelInterfaceUuid", hidden = true),
})
public class HostKernelInterfaceInventory implements Serializable {

    private String uuid;
    private String name;
    private String description;
    private String hostUuid;
    private String l2NetworkUuid;
    private String l3NetworkUuid;

    @Queryable(mappingClass = HostKernelInterfaceUsedIpInventory.class,
            joinColumn = @JoinColumn(name = "hostKernelInterfaceUuid"))
    private List<HostKernelInterfaceUsedIpInventory> usedIps;

    @Queryable(mappingClass = HostKernelInterfaceTrafficTypeInventory.class,
            joinColumn = @JoinColumn(name = "hostKernelInterfaceUuid", referencedColumnName = "trafficType"))
    private List<String> trafficTypes;

    private Timestamp createDate;
    private Timestamp lastOpDate;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public List<HostKernelInterfaceUsedIpInventory> getUsedIps() {
        return usedIps;
    }

    public void setUsedIps(List<HostKernelInterfaceUsedIpInventory> usedIps) {
        this.usedIps = usedIps;
    }

    public List<String> getTrafficTypes() {
        return trafficTypes;
    }

    public void setTrafficTypes(List<String> trafficTypes) {
        this.trafficTypes = trafficTypes;
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

    public HostKernelInterfaceInventory() {

    }

    public HostKernelInterfaceInventory(HostKernelInterfaceVO vo) {
        this.setUuid(vo.getUuid());
        this.setName(vo.getName());
        this.setDescription(vo.getDescription());
        this.setHostUuid(vo.getHostUuid());
        this.setL2NetworkUuid(vo.getL2NetworkUuid());
        this.setL3NetworkUuid(vo.getL3NetworkUuid());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setUsedIps(HostKernelInterfaceUsedIpInventory.valueOf1(vo.getUsedIps()));

        List<String> trafficTypes = new ArrayList<String>(vo.getTrafficTypes().size());
        for (HostKernelInterfaceTrafficTypeVO ref : vo.getTrafficTypes()) {
            trafficTypes.add(ref.getTrafficType().toString());
        }
        this.setTrafficTypes(trafficTypes);
    }

    public static HostKernelInterfaceInventory valueOf(HostKernelInterfaceVO vo) {
        return new HostKernelInterfaceInventory(vo);
    }

    public static List<HostKernelInterfaceInventory> valueOf1(Collection<HostKernelInterfaceVO> vos) {
        List<HostKernelInterfaceInventory> invs = new ArrayList<>(vos.size());
        for (HostKernelInterfaceVO vo : vos) {
            invs.add(new HostKernelInterfaceInventory(vo));
        }
        return invs;
    }

    public static HostKernelInterfaceInventory __example__() {
        HostKernelInterfaceInventory inv = new HostKernelInterfaceInventory();
        inv.setUuid(DocUtils.createFixedUuid(HostKernelInterfaceVO.class));
        inv.setName("host-kernel-interface");
        inv.setDescription("example");
        inv.setHostUuid(DocUtils.createFixedUuid(HostVO.class));
        inv.setL2NetworkUuid(DocUtils.createFixedUuid(L2PortGroupNetworkVO.class));
        inv.setL3NetworkUuid(DocUtils.createFixedUuid(PortGroupVO.class));
        inv.setUsedIps(Collections.singletonList(HostKernelInterfaceUsedIpInventory.__example__()));
        inv.setTrafficTypes(Collections.singletonList(HostKernelInterfaceTrafficType.Management.toString()));
        inv.setCreateDate(DocUtils.timestamp());
        inv.setLastOpDate(DocUtils.timestamp());
        return inv;
    }
}
