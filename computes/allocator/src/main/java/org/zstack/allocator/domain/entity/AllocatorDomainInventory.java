package org.zstack.allocator.domain.entity;

import org.zstack.allocator.domain.header.AllocatorDomainState;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.message.DocUtils;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.Queryable;
import org.zstack.header.search.Inventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.header.zone.ZoneVO;

import javax.persistence.JoinColumn;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = AllocatorDomainVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "zone", inventoryClass = ZoneInventory.class,
                foreignKey = "zoneUuid", expandedInventoryKey = "uuid")
})
public class AllocatorDomainInventory implements Serializable {
    private String uuid;
    private String name;
    private String description;
    private String zoneUuid;
    private String state;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    @Queryable(mappingClass = AllocatorDomainRuleInventory.class,
            joinColumn = @JoinColumn(name = "domainUuid"))
    private List<AllocatorDomainRuleInventory> rules;

    @Queryable(mappingClass = AllocatorDomainSceneInventory.class,
            joinColumn = @JoinColumn(name = "domainUuid"))
    private List<AllocatorDomainSceneInventory> scenes;

    public AllocatorDomainInventory() {
    }

    public static AllocatorDomainInventory valueOf(AllocatorDomainVO vo) {
        AllocatorDomainInventory inv = new AllocatorDomainInventory();
        inv.setUuid(vo.getUuid());
        inv.setName(vo.getName());
        inv.setDescription(vo.getDescription());
        inv.setZoneUuid(vo.getZoneUuid());
        inv.setState(vo.getState() == null ? null : vo.getState().toString());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setRules(AllocatorDomainRuleInventory.valueOf(vo.getRules()));
        inv.setScenes(AllocatorDomainSceneInventory.valueOf(vo.getScenes()));
        return inv;
    }

    public static List<AllocatorDomainInventory> valueOf(Collection<AllocatorDomainVO> vos) {
        List<AllocatorDomainInventory> invs = new ArrayList<>();
        for (AllocatorDomainVO vo : vos) {
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

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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

    public List<AllocatorDomainRuleInventory> getRules() {
        return rules;
    }

    public void setRules(List<AllocatorDomainRuleInventory> rules) {
        this.rules = rules;
    }

    public List<AllocatorDomainSceneInventory> getScenes() {
        return scenes;
    }

    public void setScenes(List<AllocatorDomainSceneInventory> scenes) {
        this.scenes = scenes;
    }

    public static AllocatorDomainInventory __example__() {
        AllocatorDomainInventory inv = new AllocatorDomainInventory();
        inv.setUuid(DocUtils.createFixedUuid(AllocatorDomainVO.class));
        inv.setName("domain-1");
        inv.setDescription("example allocator domain");
        inv.setZoneUuid(DocUtils.createFixedUuid(ZoneVO.class));
        inv.setState(AllocatorDomainState.Enabled.toString());
        inv.setCreateDate(DocUtils.timestamp());
        inv.setLastOpDate(DocUtils.timestamp());
        inv.setRules(new ArrayList<>());
        inv.setScenes(new ArrayList<>());
        return inv;
    }
}
