package org.zstack.zwatch.alarm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.datatype.Label;
import org.zstack.zwatch.namespace.VmNamespace;

@RestRequest(path = "/zwatch/events/subscriptions/{subscriptionUuid}/labels",
        method = HttpMethod.POST,
        responseClass = APIAddLabelToEventSubscriptionEvent.class,
        parameterName = "params"
)
public class APIAddLabelToEventSubscriptionMsg extends APICreateMessage implements EventSubscriptionMessage {
    @APIParam(resourceType = EventSubscriptionVO.class)
    private String subscriptionUuid;
    @APIParam(maxLength = 1024)
    private String key;
    @APIParam
    private String value;
    @APIParam(validValues = {"Regex", "Equal"})
    private String operator;

    public static APIAddLabelToEventSubscriptionMsg __example__() {
        APIAddLabelToEventSubscriptionMsg ret = new APIAddLabelToEventSubscriptionMsg();
        ret.subscriptionUuid = uuid(EventSubscriptionVO.class);
        ret.key = VmNamespace.LabelNames.VMUuid.toString();
        ret.operator = Label.Operator.Equal.name();
        ret.value = "41cf44200832452aaa35cafe5eca1ac1";
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
}
