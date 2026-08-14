package org.zstack.zwatch.alarm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.zwatch.datatype.Label;
import org.zstack.zwatch.namespace.VmNamespace;

@RestRequest(path = "/zwatch/alarms/labels/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateAlarmLabelEvent.class,
        isAction = true
)
public class APIUpdateAlarmLabelMsg extends APIMessage implements AlarmMessage {
    @APIParam(resourceType = AlarmLabelVO.class)
    private String uuid;

    @APINoSee
    private String alarmUuid;

    @APIParam(maxLength = 1024)
    private String key;

    @APIParam
    private String value;

    @APIParam(validValues = {"Regex", "Equal"})
    private String operator;

    public static APIUpdateAlarmLabelMsg __example__() {
        APIUpdateAlarmLabelMsg ret = new APIUpdateAlarmLabelMsg();
        ret.alarmUuid = uuid();
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

    public String getAlarmUuid() {
        return alarmUuid;
    }

    public void setAlarmUuid(String alarmUuid) {
        this.alarmUuid = alarmUuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
