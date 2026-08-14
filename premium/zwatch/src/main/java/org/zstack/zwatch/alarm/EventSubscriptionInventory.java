package org.zstack.zwatch.alarm;

import org.zstack.header.message.DocUtils;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.zwatch.namespace.VmNamespace;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

@Inventory(mappingVOClass = EventSubscriptionVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "labels", inventoryClass = EventSubscriptionLabelInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "subscriptionUuid"),
        @ExpandedQuery(expandedField = "actions", inventoryClass = EventSubscriptionActionInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "subscriptionUuid")
})
public class EventSubscriptionInventory {
    private String uuid;
    private String name;
    private String namespace;
    private String eventName;
    private EventSubscriptionState state;
    private List<EventSubscriptionActionInventory> actions;
    private List<EventSubscriptionLabelInventory> labels;
    private Timestamp lastOpDate;
    private Timestamp createDate;
    private String emergencyLevel;

    public static EventSubscriptionInventory __example__() {
        EventSubscriptionInventory ret = new EventSubscriptionInventory();
        ret.uuid = DocUtils.createFixedUuid(EventSubscriptionVO.class);
        ret.name = "vm_state_change_event";
        ret.namespace = "ZStack/VM";
        ret.eventName = VmNamespace.VMHAProcess.getName();
        ret.state = EventSubscriptionState.Enabled;
        ret.actions = asList(EventSubscriptionActionInventory.__example__());
        ret.labels = asList(EventSubscriptionLabelInventory.__example__());
        ret.lastOpDate = DocUtils.timestamp();
        ret.createDate = DocUtils.timestamp();
        return ret;
    }

    public static EventSubscriptionInventory valueOf(EventSubscriptionVO vo) {
        EventSubscriptionInventory inv = new EventSubscriptionInventory();
        inv.namespace = vo.getNamespace();
        inv.uuid = vo.getUuid();
        inv.name = vo.getName();
        inv.eventName = vo.getEventName();
        inv.state = vo.getState();
        inv.actions = EventSubscriptionActionInventory.valueOf(vo.getActions());
        inv.labels = EventSubscriptionLabelInventory.valueOf(vo.getLabels());
        inv.lastOpDate = vo.getLastOpDate();
        inv.createDate = vo.getCreateDate();
        if(vo.getEmergencyLevel() != null) {
            inv.setEmergencyLevel(vo.getEmergencyLevel().name());
        }
        return inv;
    }

    public static List<EventSubscriptionInventory> valueOf(Collection<EventSubscriptionVO> vos) {
        return vos.stream().map(EventSubscriptionInventory::valueOf).collect(Collectors.toList());
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public EventSubscriptionState getState() {
        return state;
    }

    public void setState(EventSubscriptionState state) {
        this.state = state;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public List<EventSubscriptionActionInventory> getActions() {
        return actions;
    }

    public void setActions(List<EventSubscriptionActionInventory> actions) {
        this.actions = actions;
    }

    public List<EventSubscriptionLabelInventory> getLabels() {
        return labels;
    }

    public void setLabels(List<EventSubscriptionLabelInventory> labels) {
        this.labels = labels;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmergencyLevel() {
        return emergencyLevel;
    }

    public void setEmergencyLevel(String emergencyLevel) {
        this.emergencyLevel = emergencyLevel;
    }
}
