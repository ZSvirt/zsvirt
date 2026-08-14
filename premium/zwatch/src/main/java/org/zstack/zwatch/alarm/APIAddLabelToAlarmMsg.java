package org.zstack.zwatch.alarm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.datatype.Label;
import org.zstack.zwatch.namespace.VmNamespace;

@RestRequest(path = "/zwatch/alarms/{alarmUuid}/labels",
        method = HttpMethod.POST,
        responseClass = APIAddLabelToAlarmEvent.class,
        parameterName = "params")
public class APIAddLabelToAlarmMsg extends APICreateMessage implements AlarmMessage {
    @APIParam(resourceType = AlarmVO.class)
    private String alarmUuid;
    @APIParam(maxLength = 1024)
    private String key;
    @APIParam
    private String value;
    @APIParam(validValues = {"Regex", "Equal"})
    private String operator;

    public static APIAddLabelToAlarmMsg __example__() {
        APIAddLabelToAlarmMsg ret = new APIAddLabelToAlarmMsg();
        ret.alarmUuid = uuid(AlarmVO.class);
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

    public String getAlarmUuid() {
        return alarmUuid;
    }

    public void setAlarmUuid(String alarmUuid) {
        this.alarmUuid = alarmUuid;
    }
}
