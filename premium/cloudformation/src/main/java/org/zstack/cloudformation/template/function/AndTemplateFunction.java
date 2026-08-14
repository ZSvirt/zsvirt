package org.zstack.cloudformation.template.function;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.zstack.cloudformation.template.struct.CfnResults;
import org.zstack.header.errorcode.OperationFailureException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.zstack.core.Platform.operr;

/**
 * Created by mingjian.deng on 2019/9/27.
 */
public class AndTemplateFunction implements TemplateFunction {
    @Override
    public Object getFunctionResult(JsonElement element, CfnResults result) {
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            checkElement(array);
            for (JsonElement e: array) {
                Boolean o;
                if (e.isJsonPrimitive()) {
                    String tmp = e.getAsString();
                    if (tmp.equals("true") || tmp.equals("false")) {
                        o = e.getAsBoolean();
                    } else {
                        // e is a Condition Name, search in conditions
                        Set<String> keys = result.getConditionParams().keySet();
                        if (keys.contains(e.getAsString())) {
                            o = result.getConditionParams().get(e.getAsString());
                        } else {
                            throw new OperationFailureException(operr("expect 'true', 'false' or an other Condition, current Conditions include: %s, but got %s",
                                    keys, e.getAsString()));
                        }
                    }
                } else {
                    Object tmp = TemplateFunctionUtils.getObject(e, result);
                    if (!(tmp instanceof Boolean)) {
                        throw new OperationFailureException(operr("expect 'true', 'false' for the object, but got %s", e.getAsString()));
                    } else {
                        o = (Boolean)tmp;
                    }
                }
                if (!o) {
                    return false;
                }
            }
            return true;
        } else {
            throw new OperationFailureException(operr(getErrorInfo()));
        }
    }

    @Override
    public List<String> getRefValue(JsonElement element, CfnResults result) {
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            checkElement(array);
            List<String> refs = new ArrayList<>();
            for (JsonElement e: array) {
                if (e.isJsonObject()) {
                    List<String> refs1 = TemplateFunctionUtils.getRef(e, result);
                    if (refs1 != null) {
                        refs.addAll(refs1);
                    }
                }
            }
            return refs;

        } else {
            throw new OperationFailureException(operr(getErrorInfo()));
        }
    }

    private void checkElement(JsonArray array) {
        if (array == null || (array.size() < 1)) {
            throw new OperationFailureException(operr(getErrorInfo()));
        }
    }

    @Override
    public String getErrorInfo() {
        return "Fn::And must be Array and contains at list 1 params";
    }
}
