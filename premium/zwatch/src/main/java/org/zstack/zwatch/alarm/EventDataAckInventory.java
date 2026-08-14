package org.zstack.zwatch.alarm;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;
import org.zstack.zwatch.ZWatchConstants;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(
        mappingVOClass = EventDataAckVO.class,
        collectionValueOfMethod = "valueOf1",
        parent = {@Parent(inventoryClass = AlertDataAckInventory.class, type = ZWatchConstants.EVENT_DATA_TYPE)}
)
public class EventDataAckInventory extends AlertDataAckInventory {
    private String eventSubscriptionUuid;

    protected EventDataAckInventory(EventDataAckVO vo) {
        super(vo);
        this.setEventSubscriptionUuid(vo.getEventSubscriptionUuid());
    }

    public static EventDataAckInventory valueOf(EventDataAckVO vo) {
        return new EventDataAckInventory(vo);
    }

    public static List<EventDataAckInventory> valueOf1(Collection<EventDataAckVO> vos) {
        List<EventDataAckInventory> invs = new ArrayList<EventDataAckInventory>(vos.size());
        for (EventDataAckVO vo : vos) {
            invs.add(EventDataAckInventory.valueOf(vo));
        }
        return invs;
    }

    public EventDataAckInventory() {
    }

    public String getEventSubscriptionUuid() {
        return eventSubscriptionUuid;
    }

    public void setEventSubscriptionUuid(String $paramName) {
        eventSubscriptionUuid = $paramName;
    }

}
