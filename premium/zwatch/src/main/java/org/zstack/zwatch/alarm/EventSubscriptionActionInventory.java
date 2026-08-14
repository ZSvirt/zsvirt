package org.zstack.zwatch.alarm;

import org.zstack.header.message.DocUtils;
import org.zstack.header.search.Inventory;
import org.zstack.sns.SNSTopicVO;
import org.zstack.zwatch.alarm.sns.SNSActionFactory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = EventSubscriptionActionVO.class)
public class EventSubscriptionActionInventory {
    private String subscriptionUuid;
    private String actionType;
    private String actionUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static EventSubscriptionActionInventory __example__() {
        EventSubscriptionActionInventory ret = new EventSubscriptionActionInventory();
        ret.subscriptionUuid = DocUtils.createFixedUuid(EventSubscriptionVO.class);
        ret.actionUuid = DocUtils.createFixedUuid(SNSTopicVO.class);
        ret.actionType = SNSActionFactory.type.toString();
        ret.lastOpDate = DocUtils.timestamp();
        ret.createDate = DocUtils.timestamp();
        return ret;
    }

    public static EventSubscriptionActionInventory valueOf(EventSubscriptionActionVO vo) {
        EventSubscriptionActionInventory inv = new EventSubscriptionActionInventory();
        inv.actionType = vo.getActionType();
        inv.actionUuid = vo.getActionUuid();
        inv.subscriptionUuid = vo.getSubscriptionUuid();
        inv.lastOpDate = vo.getLastOpDate();
        inv.createDate = vo.getCreateDate();
        return inv;
    }

    public static List<EventSubscriptionActionInventory> valueOf(Collection<EventSubscriptionActionVO> vos) {
        return vos.stream().map(EventSubscriptionActionInventory::valueOf).collect(Collectors.toList());
    }

    public String getSubscriptionUuid() {
        return subscriptionUuid;
    }

    public void setSubscriptionUuid(String subscriptionUuid) {
        this.subscriptionUuid = subscriptionUuid;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getActionUuid() {
        return actionUuid;
    }

    public void setActionUuid(String actionUuid) {
        this.actionUuid = actionUuid;
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
}
