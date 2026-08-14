package org.zstack.zwatch.alarm;

import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;
import org.zstack.zwatch.datatype.Label;
import org.zstack.zwatch.namespace.VmNamespace;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = EventSubscriptionLabelVO.class)
public class EventSubscriptionLabelInventory {
    private String uuid;
    private String key;
    private Label.Operator operator;
    private String value;
    @APINoSee
    private String subscriptionUuid;

    public static EventSubscriptionLabelInventory __example__() {
        EventSubscriptionLabelInventory ret = new EventSubscriptionLabelInventory();
        ret.key = VmNamespace.EventLabelNames.DestinationHostUuid.toString();
        ret.value = "f160255ea0a94e20a67d7a83196a26d3";
        ret.operator = Label.Operator.Equal;
        ret.uuid = DocUtils.createFixedUuid(EventSubscriptionLabelVO.class);
        return ret;
    }

    public static EventSubscriptionLabelInventory valueOf(EventSubscriptionLabelVO vo) {
        EventSubscriptionLabelInventory inv = new EventSubscriptionLabelInventory();
        inv.uuid = vo.getUuid();
        inv.key = vo.getKey();
        inv.operator = vo.getOperator();
        inv.value = vo.getValue();
        inv.subscriptionUuid = vo.getSubscriptionUuid();
        return inv;
    }

    public static List<EventSubscriptionLabelInventory> valueOf(Collection<EventSubscriptionLabelVO> vos) {
        return vos.stream().map(EventSubscriptionLabelInventory::valueOf).collect(Collectors.toList());
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Label toLabel() {
        return new Label(String.format("%s%s%s", key, operator, value));
    }

    public String getSubscriptionUuid() {
        return subscriptionUuid;
    }

    public void setSubscriptionUuid(String subscriptionUuid) {
        this.subscriptionUuid = subscriptionUuid;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Label.Operator getOperator() {
        return operator;
    }

    public void setOperator(Label.Operator operator) {
        this.operator = operator;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
