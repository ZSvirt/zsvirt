package org.zstack.cloudformation.template.function;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.zstack.cloudformation.template.struct.CfnResults;
import org.zstack.header.errorcode.OperationFailureException;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.core.Platform.operr;

/**
 * Created by mingjian.deng on 2019/9/27.
 */
public class EqualTemplateFunction implements TemplateFunction {
    @Override
    public Object getFunctionResult(JsonElement element, CfnResults result) {
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            checkElement(array);
            Object o1, o2;
            if (array.get(0).isJsonPrimitive()) {
                o1 = array.get(0).getAsString();
            } else {
                o1 = TemplateFunctionUtils.getObject(array.get(0), result);
            }

            if (array.get(1).isJsonPrimitive()) {
                o2 = array.get(1).getAsString();
            } else {
                o2 = TemplateFunctionUtils.getObject(array.get(1), result);
            }
            if (o1.toString().trim().equals(o2.toString().trim())) {
                return true;
            } else {
                return false;
            }
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
        if (array == null || (array.size() != 2)) {
            throw new OperationFailureException(operr(getErrorInfo()));
        }
    }

    @Override
    public String getErrorInfo() {
        return "Fn::Equal must be Array and contains 2 params, the first param must be JsonPrimitive!";
    }
}
