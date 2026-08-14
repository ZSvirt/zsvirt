package org.zstack.cloudformation.template.decoder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.zstack.cloudformation.template.CloudFormationDecoder;
import org.zstack.cloudformation.template.function.TemplateFunctionUtils;
import org.zstack.cloudformation.template.struct.CfnResults;
import org.zstack.cloudformation.template.struct.CloudFormationConstants;
import org.zstack.cloudformation.template.struct.OutputStruct;
import org.zstack.header.errorcode.OperationFailureException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.zstack.core.Platform.operr;

/**
 * Created by mingjian.deng on 2018/5/28.
 */
public class OutputDecoder extends AbstractCfnRootDecoder {
    private OutputStruct getOutputFromResult(CfnResults result, String name) {
        for (OutputStruct s: result.getOutputs()) {
            if (s.getOutputName().equals(name)) {
                return s;
            }
        }
        return null;
    }

    private Object decodeValue(JsonElement element, CfnResults result) {
        if (element.isJsonPrimitive()) {
            return CloudFormationDecoder.getValueByType((JsonPrimitive)element);
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            List<Object> o = new ArrayList<>();
            if (array == null) {
                return o;
            } else {
                for (JsonElement e: array) {
                    o.add(decodeValue(e, result));
                }
                return o;
            }
        } else if (element.isJsonObject()) {
            Set<Map.Entry<String, JsonElement>> es = element.getAsJsonObject().entrySet();
            for (Map.Entry<String, JsonElement> e: es) {
                if (CloudFormationConstants.functions.contains(e.getKey())) {
                    return TemplateFunctionUtils.getFunctions(e.getKey()).getFunctionResult(e.getValue(), result);
                } else {
                    throw new OperationFailureException(operr(String.format("only functions can be allowed in Outputs, but found %s", e.getKey())));
                }
            }
        }
        throw new OperationFailureException(operr("Output body cannot support json null!"));
    }

    private void decodeOutputs(JsonElement element, CfnResults result, String name) {
        if (element.isJsonObject()) {
            Set<Map.Entry<String, JsonElement>> es = element.getAsJsonObject().entrySet();
            es.forEach(e -> {
                OutputStruct p = getOutputFromResult(result, name);
                if (p == null) {
                    throw new OperationFailureException(operr("resourceName must be found in result, or it is invalid cfn json."));
                }
                if (e.getKey().equals("Description")) {
                    if (!e.getValue().isJsonPrimitive()) {
                        throw new OperationFailureException(operr("Description in Outputs must be String type!"));
                    }
                    p.setDescription(e.getValue().getAsString());
                } else if (e.getKey().equals("Value")) {
                    Object o = decodeValue(e.getValue(), result);

                    p.setContent(o);
                } else {
                    throw new OperationFailureException(operr(String.format("[%s] is invalid in Outputs!", e.getKey())));
                }
            });
        } else if (element.isJsonPrimitive()) {
            throw new OperationFailureException(operr("Mapping body cannot support non map value!"));
        } else {
            throw new OperationFailureException(operr("Mapping body cannot support json null!"));
        }
    }

    @Override
    public void decode(JsonElement element, CfnResults result) {
//        CloudFormationDecoder.printTree(element);
        if (element.isJsonObject()) {
            Set<Map.Entry<String, JsonElement>> es = element.getAsJsonObject().entrySet();
            es.forEach(e -> {
                OutputStruct p = new OutputStruct();
                p.setOutputName(e.getKey());
                result.getOutputs().add(p);
                decodeOutputs(e.getValue(), result, e.getKey());
            });
        } else {
            throw new OperationFailureException(operr("Mappings root body must be json object!"));
        }
    }
}
