package org.zstack.zwatch.resnotify;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.search.Inventory;
import org.zstack.core.db.Q;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;



@Inventory(mappingVOClass = ResNotifySubscriptionVO.class)
@ExpandedQueries({})
public class ResNotifySubscriptionInventory {
    private String uuid;
    private String name;
    private String description;
    private String resourceTypes;
    private String eventTypes;
    private ResNotifyType type;
    private ResNotifySubscriptionState state;
    private String accountUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private ResNotifyWebhookRefInventory webhookRef;

    public static ResNotifySubscriptionInventory valueOf(ResNotifySubscriptionVO vo) {
        ResNotifySubscriptionInventory inv = new ResNotifySubscriptionInventory();
        inv.setUuid(vo.getUuid());
        inv.setName(vo.getName());
        inv.setDescription(vo.getDescription());
        inv.setResourceTypes(vo.getResourceTypes());
        inv.setEventTypes(vo.getEventTypes());
        inv.setType(vo.getType());
        inv.setState(vo.getState());
        inv.setAccountUuid(vo.getAccountUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        if (vo.getType() == ResNotifyType.WEBHOOK) {
            ResNotifyWebhookRefVO ref = Q.New(ResNotifyWebhookRefVO.class)
                    .eq(ResNotifyWebhookRefVO_.uuid, vo.getUuid())
                    .find();
            if (ref != null) {
                inv.setWebhookRef(ResNotifyWebhookRefInventory.valueOf(ref));
            }
        }
        return inv;
    }

    public static List<ResNotifySubscriptionInventory> valueOf(Collection<ResNotifySubscriptionVO> vos) {
        List<ResNotifySubscriptionInventory> invs = new ArrayList<>();
        for (ResNotifySubscriptionVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    // Getters and Setters

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

    public String getResourceTypes() {
        return resourceTypes;
    }

    public void setResourceTypes(String resourceTypes) {
        this.resourceTypes = resourceTypes;
    }

    public String getEventTypes() {
        return eventTypes;
    }

    public void setEventTypes(String eventTypes) {
        this.eventTypes = eventTypes;
    }

    public ResNotifyType getType() {
        return type;
    }

    public void setType(ResNotifyType type) {
        this.type = type;
    }

    public ResNotifySubscriptionState getState() {
        return state;
    }

    public void setState(ResNotifySubscriptionState state) {
        this.state = state;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
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

    public ResNotifyWebhookRefInventory getWebhookRef() {
        return webhookRef;
    }

    public void setWebhookRef(ResNotifyWebhookRefInventory webhookRef) {
        this.webhookRef = webhookRef;
    }
}
