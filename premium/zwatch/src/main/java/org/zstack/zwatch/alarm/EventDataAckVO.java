package org.zstack.zwatch.alarm;

import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.ToInventory;

import javax.persistence.*;

/**
 * Create by lining at 2020/10/10
 */
@Entity
@Table
@AutoDeleteTag
@PrimaryKeyJoinColumn(name="alertDataUuid", referencedColumnName = "alertDataUuid")
public class EventDataAckVO extends AlertDataAckVO implements ToInventory {
    @Column
    private String eventSubscriptionUuid;

    public String getEventSubscriptionUuid() {
        return eventSubscriptionUuid;
    }

    public void setEventSubscriptionUuid(String eventSubscriptionUuid) {
        this.eventSubscriptionUuid = eventSubscriptionUuid;
    }
}
