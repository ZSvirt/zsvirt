package org.zstack.vrouterRoute;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.*;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;

import javax.persistence.JoinColumn;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.*;

/**
 * Created by weiwang on 16/06/2017.
 */
@PythonClassInventory
@Inventory(mappingVOClass = VRouterRouteTableVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "attachedRouterRef", inventoryClass = VirtualRouterVRouterRouteTableRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "routeTableUuid"),
        @ExpandedQuery(expandedField = "routeEntries", inventoryClass = VRouterRouteEntryInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "routeTableUuid"),
})
public class VRouterRouteTableInventory implements Serializable {
    private String uuid;

    private String name;

    private String description;

    @APINoSee
    private String type;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    @Queryable(mappingClass = VirtualRouterVRouterRouteTableRefInventory.class,
            joinColumn = @JoinColumn(name = "routeTableUuid"))
    private List<VirtualRouterVRouterRouteTableRefInventory> attachedRouterRefs;

    @Queryable(mappingClass = VRouterRouteEntryInventory.class,
            joinColumn = @JoinColumn(name = "routeTableUuid"))
    private List<VRouterRouteEntryInventory> routeEntries;

    public VRouterRouteTableInventory() {
    }

    protected VRouterRouteTableInventory(VRouterRouteTableVO vo) {
        setUuid(vo.getUuid());
        setName(vo.getName());
        setDescription(vo.getDescription());
        setType(vo.getType());
        setAttachedRouterRefs(VirtualRouterVRouterRouteTableRefInventory.valueOf(vo.getAttachedRouterRefs()));
        setRouteEntries(VRouterRouteEntryInventory.valueOf(vo.getRouteEntries()));
        setCreateDate(vo.getCreateDate());
        setLastOpDate(vo.getLastOpDate());
    }

    public static VRouterRouteTableInventory valueOf(VRouterRouteTableVO vo) {
        return new VRouterRouteTableInventory(vo);
    }

    public static List<VRouterRouteTableInventory> valueOf(Collection<VRouterRouteTableVO> vos) {
        List<VRouterRouteTableInventory> invs = new ArrayList<>(vos.size());
        for (VRouterRouteTableVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public List<VirtualRouterVRouterRouteTableRefInventory> getAttachedRouterRefs() {
        return attachedRouterRefs;
    }

    public void setAttachedRouterRefs(List<VirtualRouterVRouterRouteTableRefInventory> attachedRouterRefs) {
        this.attachedRouterRefs = attachedRouterRefs;
    }


    public List<VRouterRouteEntryInventory> getRouteEntries() {
        return routeEntries;
    }

    public void setRouteEntries(List<VRouterRouteEntryInventory> routeEntries) {
        this.routeEntries = routeEntries;
    }
}
