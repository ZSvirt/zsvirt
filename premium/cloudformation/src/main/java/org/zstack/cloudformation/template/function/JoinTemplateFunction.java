package org.zstack.cloudformation.template.function;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.zstack.cloudformation.template.struct.CfnResults;
import org.zstack.header.errorcode.OperationFailureException;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.core.Platform.operr;

/**
 * Created by mingjian.deng on 2018/5/31.
 */
public class JoinTemplateFunction implements TemplateFunction {
    @Override
    public Object getFunctionResult(JsonElement element, CfnResults result) {
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            checkElement(array);
            String binder = array.get(0).getAsString();
            JsonArray array1 = array.get(1).getAsJsonArray();
            StringBuilder builder = new StringBuilder();
            for (int index = 0; index < array1.size(); index ++) {
                if (array1.get(index).isJsonObject()) {
                    builder.append(TemplateFunctionUtils.getString(array1.get(index), result, false));
                } else {
                    builder.append(array1.get(index).getAsString());
                }
                if (index + 1 < array1.size()) {
                    builder.append(binder);
                }
            }
            return builder.toString();
//            return builder.deleteCharAt(builder.lastIndexOf(binder)).toString();
        } else {
            throw new OperationFailureException(operr(getErrorInfo()));
        }
    }

    private void checkElement(JsonArray array) {
        if (array == null || array.size() != 2) {
            throw new OperationFailureException(operr(getErrorInfo()));
        }
        if (!array.get(0).isJsonPrimitive()) {
            throw new OperationFailureException(operr(getErrorInfo()));
        }
        if (!array.get(1).isJsonArray()) {
            throw new OperationFailureException(operr(getErrorInfo()));
        }
    }

    @Override
    public List<String> getRefValue(JsonElement element, CfnResults result) {
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            checkElement(array);
            List<String> refs = new ArrayList<>();
            JsonArray array1 = array.get(1).getAsJsonArray();
            for (int index = 0; index < array1.size(); index ++) {
                if (array1.get(index).isJsonObject()) {
                    List<String> tmp = TemplateFunctionUtils.getRef(array1.get(index), result);
                    if (tmp != null) {
                        refs.addAll(tmp);
                    }
                }
            }
            return refs;
        }
        return null;
    }

    @Override
    public String getErrorInfo() {
        return "Fn::Join must be array and contain 2 params, array[0] must be String, array[1] must be array!";
    }
}
