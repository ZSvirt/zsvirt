package org.zstack.vrouterRoute;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by weiwang on 16/06/2017.
 */
@PythonClassInventory
@Inventory(mappingVOClass = VRouterRouteEntryVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "vrouterRouteTable", inventoryClass = VRouterRouteTableInventory.class,
                foreignKey = "routeTableUuid", expandedInventoryKey = "uuid"),
})
public class VRouterRouteEntryInventory implements Serializable {
    private String uuid;

    private String description;

    private VRouterRouteEntryType type;

    private String routeTableUuid;

    private String destination;

    private String target;

    private Integer distance;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    public VRouterRouteEntryInventory() {
    }

    protected VRouterRouteEntryInventory(VRouterRouteEntryVO vo) {
        setUuid(vo.getUuid());
        setDescription(vo.getDescription());
        setDestination(vo.getDestination());
        setTarget(vo.getTarget());
        setType(vo.getType());
        setRouteTableUuid(vo.getRouteTableUuid());
        setDistance(vo.getDistance());
        setCreateDate(vo.getCreateDate());
        setLastOpDate(vo.getLastOpDate());
    }

    public static VRouterRouteEntryInventory valueOf(VRouterRouteEntryVO vo) {
        return new VRouterRouteEntryInventory(vo);
    }

    public static List<VRouterRouteEntryInventory> valueOf(Collection<VRouterRouteEntryVO> vos) {
        List<VRouterRouteEntryInventory> invs = new ArrayList<>(vos.size());
        for (VRouterRouteEntryVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public VRouterRouteEntryType getType() {
        return type;
    }

    public void setType(VRouterRouteEntryType type) {
        this.type = type;
    }

    public String getRouteTableUuid() {
        return routeTableUuid;
    }

    public void setRouteTableUuid(String routeTableUuid) {
        this.routeTableUuid = routeTableUuid;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
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

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Integer getDistance() {
        return distance;
    }

    public void setDistance(Integer distance) {
        this.distance = distance;
    }
}
