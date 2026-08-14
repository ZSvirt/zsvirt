package org.zstack.cloudformation.template.decoder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.zstack.cloudformation.template.CloudFormationDecoder;
import org.zstack.cloudformation.template.struct.CfnResults;
import org.zstack.cloudformation.template.struct.CloudFormationConstants;
import org.zstack.cloudformation.template.struct.ParameterStruct;
import org.zstack.header.errorcode.OperationFailureException;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import static org.zstack.core.Platform.operr;

/**
 * Created by mingjian.deng on 2018/5/28.
 */
public class ParameterDecoder extends AbstractCfnRootDecoder {
    private Stack<String> keys = new Stack<>();

    protected ParameterStruct getParamFromResult(CfnResults result, String paraName) {
        for (ParameterStruct s: result.getParams()) {
            if (s.getParamName().equals(paraName)) {
                return s;
            }
        }
        return null;
    }

    private void setField(ParameterStruct param, String key, Object value) {
        try {
            Field f = ParameterStruct.class.getDeclaredField(key);
            f.setAccessible(true);
            f.set(param, value);
        } catch (ReflectiveOperationException e) {
            throw new OperationFailureException(operr(e.getMessage()));
        }
    }

    protected void decodeParams(JsonElement element, CfnResults result, String paramName) {
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            if (array == null) {
                return;
            }
            for (JsonElement e: array) {
                decodeParams(e, result, paramName);
            }
        } else if (element.isJsonObject()) {
            Set<Map.Entry<String, JsonElement>> es = element.getAsJsonObject().entrySet();
            es.forEach(e -> {
                if (e.getKey().equals("DefaultValue")) {
                    ParameterStruct p = getParamFromResult(result, paramName);
                    if (p == null) {
                        throw new OperationFailureException(operr("paramName must be found in result, or it is invalid cfn json."));
                    }
                    setField(p, CloudFormationDecoder.doConvert(e.getKey()), CloudFormationDecoder.getValueByType(p.getType(), e.getValue()));
                } else {
                    keys.push(e.getKey());
                    decodeParams(e.getValue(), result, paramName);
                }
            });
        } else if (element.isJsonPrimitive()) {
            ParameterStruct p = getParamFromResult(result, paramName);
            if (p == null) {
                throw new OperationFailureException(operr("paramName must be found in result, or it is invalid cfn json."));
            }
            String key = keys.pop();
            if (CloudFormationConstants.paramKeys.contains(key)) {
                JsonPrimitive o = (JsonPrimitive)element;
                if (o.isBoolean()) {
                    setField(p, CloudFormationDecoder.doConvert(key), element.getAsBoolean());
                } else if (o.isNumber()) {
                    setField(p, CloudFormationDecoder.doConvert(key), element.getAsNumber());
                } else {
                    setField(p, CloudFormationDecoder.doConvert(key), element.getAsString());
                }
            } else {
                throw new OperationFailureException(operr(String.format("paramName not support: %s", key)));
            }
        } else {
            throw new OperationFailureException(operr("Parameters body cannot support null!"));
        }
    }

    protected void decodeType(JsonElement element, CfnResults result, String paramName) {
        boolean found = false;
        if (element.isJsonObject()) {
            Set<Map.Entry<String, JsonElement>> es = element.getAsJsonObject().entrySet();
            for (Map.Entry<String, JsonElement> e: es) {
                if (e.getKey().equals("Type")) {
                    keys.push(e.getKey());
                    decodeParams(e.getValue(), result, paramName);
                    found = true;
                }
            }
        }
        if (!found) {
            throw new OperationFailureException(operr(String.format("Type is required for Parameters, but not found in %s", paramName)));
        }
    }

    @Override
    public void decode(JsonElement element, CfnResults result) {
//        CloudFormationDecoder.printTree(element);
        if (element.isJsonObject()) {
            Set<Map.Entry<String, JsonElement>> es = element.getAsJsonObject().entrySet();
            es.forEach(e -> {
                ParameterStruct p = null;
                for (ParameterStruct t: result.getParams()) {
                    if (t.getParamName().equals(e.getKey())) {
                        p = t;
                    }
                }
                if (p == null) {
                    p = new ParameterStruct();
                    p.setParamName(e.getKey());
                    result.getParams().add(p);
                }

                decodeType(e.getValue(), result, e.getKey());
                decodeParams(e.getValue(), result, e.getKey());
            });
        } else {
            throw new OperationFailureException(operr("Parameters root body must be json object!"));
        }
    }
}
