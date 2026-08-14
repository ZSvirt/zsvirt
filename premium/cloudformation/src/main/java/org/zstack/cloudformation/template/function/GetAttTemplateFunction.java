package org.zstack.cloudformation.template.function;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.zstack.cloudformation.template.struct.CfnResults;
import org.zstack.cloudformation.template.struct.CloudFormationConstants;
import org.zstack.cloudformation.template.struct.ResourceStruct;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.utils.CollectionDSL;

import java.util.List;

import static org.zstack.core.Platform.operr;

/**
 * Created by mingjian.deng on 2018/5/31.
 */
public class GetAttTemplateFunction implements TemplateFunction {
    @Override
    public Object getFunctionResult(JsonElement element, CfnResults result) {
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            checkElement(array);

            String p1 = array.get(0).toString();
            String p2 = array.get(1).getAsString();
            if (array.get(0).isJsonObject()) {
                p1 = TemplateFunctionUtils.getString(array.get(0), result, false);
            } else if (array.get(0).isJsonPrimitive()){
                p1 = TemplateFunctionUtils.getString(array.get(0).getAsString(), result);
            }

            if (p1.startsWith("${")) {
                return "${" + p1 + CloudFormationConstants.split + p2 + "}";
            }

            for (ResourceStruct s: result.getResources()) {
                if (s.getResourceName().equals(p1)) {
                    return "${" + p1 + CloudFormationConstants.split + p2 + "}";
                }
            }
            throw new OperationFailureException(operr(String.format("Fn::GetAtt cannot find resources [%s] in Resources!", array.get(0))));
        } else {
            throw new OperationFailureException(operr(getErrorInfo()));
        }
    }


    private void checkElement(JsonArray array) {
        if (array == null || array.size() != 2) {
            throw new OperationFailureException(operr(getErrorInfo()));
        }
        if (!array.get(1).isJsonPrimitive()) {
            throw new OperationFailureException(operr(getErrorInfo()));
        }
        JsonPrimitive e = (JsonPrimitive)array.get(1);
        if (!e.isString()) {
            throw new OperationFailureException(operr(getErrorInfo()));
        }
    }

    @Override
    public List<String> getRefValue(JsonElement element, CfnResults result) {
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            checkElement(array);
            if (array.get(0).isJsonObject()) {
                return TemplateFunctionUtils.getRef(array.get(0), result);
            } else if (array.get(0).isJsonPrimitive()) {
                return CollectionDSL.list(TemplateFunctionUtils.getString(array.get(0).getAsString(), result));
            }
        }
        return null;
    }

    @Override
    public String getErrorInfo() {
        return "Fn::GetAtt must be Array and contain 2 params, and the 2nd param must be String!";
    }
}
