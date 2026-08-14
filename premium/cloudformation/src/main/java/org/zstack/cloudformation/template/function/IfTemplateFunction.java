package org.zstack.cloudformation.template.function;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.zstack.cloudformation.template.struct.CfnResults;
import org.zstack.header.errorcode.OperationFailureException;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.core.Platform.operr;

/**
 * Created by mingjian.deng on 2019/9/26.
 */
public class IfTemplateFunction implements TemplateFunction {
    private Object getValue(JsonElement element, CfnResults result) {
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        } else if (element.isJsonArray()) {
            JsonArray array =  element.getAsJsonArray();
            List<Object> r = new ArrayList<>();
            for (JsonElement e: array) {
                r.add(getValue(e, result));
            }
            return r;
        } else {
            return TemplateFunctionUtils.getObject(element, result);
        }
    }
    @Override
    public Object getFunctionResult(JsonElement element, CfnResults result) {
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            checkElement(array);
            String cond = array.get(0).getAsString();
            if (cond.equals("true")||cond.equals("false")) {
                return Boolean.valueOf(cond);
            } else {
                Boolean r = result.getConditionParams().get(cond);
                if (r == null) {
                    throw new OperationFailureException(operr("cannot find condition[%s] in 'Conditions'", cond));
                }
                if (r) {
                    return getValue(array.get(1), result);
                } else {
                    return getValue(array.get(2), result);
                }
            }
        } else {
            throw new OperationFailureException(operr(getErrorInfo()));
        }
    }

    private void checkElement(JsonArray array) {
        if (array == null || array.size() != 3) {
            throw new OperationFailureException(operr(getErrorInfo()));
        }
        if (!array.get(0).isJsonPrimitive()) {
            throw new OperationFailureException(operr(getErrorInfo()));
        }
    }

    @Override
    public List<String> getRefValue(JsonElement element, CfnResults result) {
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            checkElement(array);
            List<String> refs = new ArrayList<>();
            if (array.get(1).isJsonObject()) {
                List<String> ref = TemplateFunctionUtils.getRef(array.get(1), result);
                if (ref != null) {
                    refs.addAll(ref);
                }
            }

            if (array.get(2).isJsonObject()) {
                List<String> ref = TemplateFunctionUtils.getRef(array.get(2), result);
                if (ref != null) {
                    refs.addAll(ref);
                }
            }
            return refs;
        } else {
            throw new OperationFailureException(operr(getErrorInfo()));
        }
    }

    @Override
    public String getErrorInfo() {
        return "Fn::If must be Array and contain 3 params, the 1st params must String!";
    }
}
