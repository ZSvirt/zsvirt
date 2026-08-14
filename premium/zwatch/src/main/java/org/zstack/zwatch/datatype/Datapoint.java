package org.zstack.zwatch.datatype;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.SDK;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.zwatch.namespace.VmNamespace;
import org.zstack.zwatch.ruleengine.ComparisonOperator;

import java.util.HashMap;
import java.util.Map;

@SDK
public class Datapoint {
    protected Double value;
    protected Long time;
    protected Map<String, String> labels;

    public static Datapoint __example__() {
        Datapoint data = new Datapoint();
        data.setValue(101.02);
        data.setTime(DocUtils.date);
        HashMap<String, String> labels = new HashMap<>();
        labels.put(VmNamespace.LabelNames.VMUuid.toString(), DocUtils.createFixedUuid(VmInstanceVO.class));
        data.labels = labels;
        return data;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public boolean compare(ComparisonOperator op, Number target) {
        double v = target.doubleValue();

        if (op == ComparisonOperator.GreaterThan) {
            return value > v;
        } else if (op == ComparisonOperator.GreaterThanOrEqualTo) {
            return value >= v;
        } else if (op == ComparisonOperator.LessThan) {
            return value < v;
        } else if (op == ComparisonOperator.LessThanOrEqualTo) {
            return value <= v;
        } else {
            throw new CloudRuntimeException(String.format("unsupported ComparisonOperator[%s]", op));
        }
    }
}
