package org.zstack.header.affinitygroup;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.query.Queryable;
import org.zstack.header.zone.ZoneInventory;

import javax.persistence.JoinColumn;
import java.io.Serializable;
import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by shixin on 10/24/2015.
 */
@PythonClassInventory
@Inventory(mappingVOClass = AffinityGroupVO.class)
public class AffinityGroupInventory implements Serializable {
    private String uuid;
    private String name;
    private String description;
    private String policy;
    private String version;
    private String type;
    private String appliance;
    private String zoneUuid;
    private String state;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    @Queryable(mappingClass = AffinityGroupUsageInventory.class,
            joinColumn = @JoinColumn(name = "affinityGroupUuid"))
    private List<AffinityGroupUsageInventory> usages;


    public AffinityGroupInventory() {
    }

    public AffinityGroupInventory(AffinityGroupVO vo) {
        this.setUuid(vo.getUuid());
        this.setName(vo.getName());
        this.setDescription(vo.getDescription());
        this.setPolicy(vo.getPolicy().toString());
        this.setVersion(vo.getVersion());
        this.setType(vo.getType().toString());
        this.setAppliance(vo.getAppliance());
        this.setState(vo.getState().toString());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setZoneUuid(vo.getZoneUuid());
        this.setUsages(AffinityGroupUsageInventory.valueOf(vo.getUsages()));
    }


    public static AffinityGroupInventory valueOf(AffinityGroupVO vo) {
        AffinityGroupInventory inv = new AffinityGroupInventory();
        inv.setUuid(vo.getUuid());
        inv.setName(vo.getName());
        inv.setDescription(vo.getDescription());
        inv.setPolicy(vo.getPolicy().toString());
        inv.setVersion(vo.getVersion());
        inv.setType(vo.getType().toString());
        inv.setAppliance(vo.getAppliance());
        inv.setState(vo.getState().toString());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setZoneUuid(vo.getZoneUuid());
        inv.setUsages(AffinityGroupUsageInventory.valueOf(vo.getUsages()));
        return inv;
    }

    public static List<AffinityGroupInventory> valueOf(Collection<AffinityGroupVO> vos) {
        List<AffinityGroupInventory> invs = new ArrayList<AffinityGroupInventory>();
        for (AffinityGroupVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
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

    public List<AffinityGroupUsageInventory> getUsages() {
        return usages;
    }

    public void setUsages(List<AffinityGroupUsageInventory> usages) {
        this.usages = usages;
    }

    public String getAppliance() {
        return appliance;
    }

    public void setAppliance(String appliance) {
        this.appliance = appliance;
    }

    public boolean isHardPolicy(){
        return policy.toString().equals(AffinityGroupPolicy.ANTIHARD.toString())
                || policy.toString().equals(AffinityGroupPolicy.AFFINITYHARD.toString());
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }
}
