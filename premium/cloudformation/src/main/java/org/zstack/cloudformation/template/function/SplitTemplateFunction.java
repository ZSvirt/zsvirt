package org.zstack.cloudformation.template.function;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.zstack.cloudformation.template.struct.CfnResults;
import org.zstack.header.errorcode.OperationFailureException;

import java.util.Arrays;
import java.util.List;

import static org.zstack.core.Platform.operr;

/**
 * Created by mingjian.deng on 2018/6/4.
 */
public class SplitTemplateFunction implements TemplateFunction {
    @Override
    public Object getFunctionResult(JsonElement element, CfnResults result) {
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            checkElement(array);
            String seperator = array.get(0).getAsString();

            return Arrays.asList(array.get(1).getAsString().split(seperator));
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
    }

    @Override
    public List<String> getRefValue(JsonElement element, CfnResults result) {
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            checkElement(array);
            if (array.get(1).isJsonObject()) {
                return TemplateFunctionUtils.getRef(array.get(1), result);
            }
        }
        return null;
    }

    @Override
    public String getErrorInfo() {
        return "Fn::Split must be Array and contain 2 params, array[0] must be String!";
    }
}
