package org.zstack.zwatch.alarm;

import org.springframework.http.HttpMethod;
import org.zstack.core.Platform;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.zwatch.datatype.Label;
import org.zstack.zwatch.namespace.VmNamespace;

/**
 * Created by MaJin on 2019/12/11.
 */

@RestRequest(path = "/zwatch/events/subscriptions/labels/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateEventSubscriptionLabelEvent.class,
        isAction = true
)
public class APIUpdateEventSubscriptionLabelMsg extends APIMessage implements EventSubscriptionMessage {
    @APIParam(resourceType = EventSubscriptionLabelVO.class)
    private String uuid;
    @APIParam(maxLength = 1024)
    private String key;
    @APIParam
    private String value;
    @APIParam(validValues = {"Regex", "Equal"})
    private String operator;

    @APINoSee
    private String subscriptionUuid;

    public static APIUpdateEventSubscriptionLabelMsg __example__() {
        APIUpdateEventSubscriptionLabelMsg ret = new APIUpdateEventSubscriptionLabelMsg();
        ret.uuid = uuid(EventSubscriptionLabelVO.class);
        ret.key = VmNamespace.LabelNames.VMUuid.toString();
        ret.operator = Label.Operator.Equal.name();
        ret.value = uuid(VmInstanceVO.class);
        return ret;
    }

    public Label.Operator getLabelOperator() {
        return Label.Operator.valueOf(operator);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public void setSubscriptionUuid(String subscriptionUuid) {
        this.subscriptionUuid = subscriptionUuid;
    }

    @Override
    public String getSubscriptionUuid() {
        return subscriptionUuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
