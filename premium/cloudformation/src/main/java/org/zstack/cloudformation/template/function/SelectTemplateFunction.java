package org.zstack.cloudformation.template.function;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.zstack.cloudformation.template.struct.CfnResults;
import org.zstack.cloudformation.template.struct.CloudFormationConstants;
import org.zstack.header.errorcode.OperationFailureException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.zstack.core.Platform.operr;

/**
 * Created by mingjian.deng on 2018/6/6.
 */
public class SelectTemplateFunction implements TemplateFunction {
    private Object getValueByType(JsonElement e, CfnResults result) {
        if (e.isJsonPrimitive()) {
            JsonPrimitive tmp = (JsonPrimitive)e;
            if (tmp.isNumber()) {
                return tmp.getAsNumber();
            } else if (tmp.isBoolean()) {
                return tmp.getAsBoolean();
            } else {
                return tmp.getAsString();
            }
        } else if (e.isJsonObject()) {
            return TemplateFunctionUtils.getString(e, result, false);
        }
        throw new OperationFailureException(operr(getErrorInfo()));
    }

    @Override
    public Object getFunctionResult(JsonElement element, CfnResults result) {
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            checkElement(array);
            if (array.get(1).isJsonObject()) {
                JsonPrimitive p = (JsonPrimitive)array.get(0);
                if (p.isNumber()) {
                    Object object = TemplateFunctionUtils.getObject(array.get(1), result);
                    if (object instanceof List) {
                        List o = (List)object;
                        return o.get(p.getAsNumber().intValue());
                    } else {
                        return "${" + object + CloudFormationConstants.split + p.getAsNumber().intValue() + "}";
                    }
                } else {
                    String key = array.get(0).getAsString();
                    Set<Map.Entry<String, JsonElement>> es = array.get(1).getAsJsonObject().entrySet();
                    for (Map.Entry<String, JsonElement> e: es) {
                        if (e.getKey().equals(key)) {
                            return getValueByType(e.getValue(), result);
                        }
                    }
                }
            } else if (array.get(1).isJsonArray()) {
                Integer index = array.get(0).getAsNumber().intValue();
                JsonElement e = array.get(1).getAsJsonArray().get(index);
                return getValueByType(e, result);
            }
        }
        throw new OperationFailureException(operr(getErrorInfo()));
    }

    private void checkElement(JsonArray array) {
        if (array == null || array.size() != 2) {
            throw new OperationFailureException(operr(getErrorInfo()));
        }
        if (!array.get(0).isJsonPrimitive()) {
            throw new OperationFailureException(operr(getErrorInfo()));
        }
        if (array.get(1).isJsonPrimitive() || array.get(1).isJsonNull()) {
            throw new OperationFailureException(operr(getErrorInfo()));
        }
        if (array.get(1).isJsonArray()) {
            Integer index = array.get(0).getAsNumber().intValue();
            if (array.get(1).getAsJsonArray().size() <= index) {
                throw new OperationFailureException(operr("Fn::Select out of range, please check your json file!"));
            }
        }
    }

    @Override
    public List<String> getRefValue(JsonElement element, CfnResults result) {
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            checkElement(array);
            if (array.get(1).isJsonObject()) {
                return TemplateFunctionUtils.getRef(array.get(1), result);
            } else if (array.get(1).isJsonArray()) {
                List<String> refs = new ArrayList<>();
                for (JsonElement e: array.get(1).getAsJsonArray()) {
                    if (e.isJsonObject()) {
                        List<String> tmp = TemplateFunctionUtils.getRef(e, result);
                        if (tmp != null) {
                            refs.addAll(tmp);
                        }
                    }
                }
                return refs;
            }
        }
        return null;
    }

    @Override
    public String getErrorInfo() {
        return "Fn::Select must be JsonArray and contains 2 params, the first param must be String!";
    }
}
