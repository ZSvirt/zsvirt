package org.zstack.core.eventlog;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = EventLogVO.class)
public class EventLogInventory {
    private long id;
    private String content;
    private String resourceUuid;
    private String resourceType;
    private String category;
    private String trackingId;
    private String type;
    private long time;
    private Timestamp createDate;

    public static EventLogInventory valueOf(EventLogVO vo) {
        EventLogInventory inv = new EventLogInventory();
        inv.id = vo.getId();
        inv.time = vo.getTime();
        inv.content = vo.getContent();
        inv.category = vo.getCategory();
        inv.resourceUuid = vo.getResourceUuid();
        inv.resourceType = vo.getResourceType();
        inv.type = vo.getType().toString();
        inv.trackingId = vo.getTrackingId();
        inv.createDate = vo.getCreateDate();
        return inv;
    }

    public static List<EventLogInventory> valueOf(Collection<EventLogVO> vos) {
        return vos.stream().map(EventLogInventory::valueOf).collect(Collectors.toList());
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public void setTrackingId(String trackingId) {
        this.trackingId = trackingId;
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
}
