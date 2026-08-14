package org.zstack.cloudformation.template.decoder;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.zstack.cloudformation.template.CloudFormationDecoder;
import org.zstack.cloudformation.template.function.TemplateFunctionUtils;
import org.zstack.cloudformation.template.struct.CfnResults;
import org.zstack.cloudformation.template.struct.CloudFormationConstants;
import org.zstack.cloudformation.template.struct.MappingStruct;
import org.zstack.header.errorcode.OperationFailureException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.zstack.core.Platform.operr;

/**
 * Created by mingjian.deng on 2018/5/28.
 */
public class MappingDecoder extends AbstractCfnRootDecoder {
    private MappingStruct getMapFromResult(CfnResults result, String name) {
        for (MappingStruct s: result.getMappings()) {
            if (s.getMappingName().equals(name)) {
                return s;
            }
        }
        return null;
    }

    private Object getMappingValue(JsonPrimitive element) {
        if (element.isNumber()) {
            return element.getAsNumber();
        } else {
            return element.getAsString();
        }
    }

    private Object decodeValues(JsonElement element, CfnResults result) {
        if (element.isJsonPrimitive()) {
            return CloudFormationDecoder.getValueByType((JsonPrimitive)element);
        } else if (element.isJsonObject()) {
            Set<Map.Entry<String, JsonElement>> es = element.getAsJsonObject().entrySet();
            Map<String, Object> values = new HashMap<>();
            for (Map.Entry<String, JsonElement> e: es) {
                if (CloudFormationConstants.functions.contains(e.getKey())) {
                    return TemplateFunctionUtils.getFunctions(e.getKey()).getFunctionResult(e.getValue(), result);
                } else {
                    values.put(e.getKey(), decodeValues(e.getValue(), result));
                }
            }
            return values;
        } else if (element.isJsonArray()) {
            throw new OperationFailureException(operr("Mapping value body cannot support json array!"));
        } else {
            throw new OperationFailureException(operr("Mapping value body cannot support null!"));
        }
    }

    private void decodeMappings(JsonElement element, CfnResults result, String name) {
        if (element.isJsonObject()) {
            Set<Map.Entry<String, JsonElement>> es = element.getAsJsonObject().entrySet();
            es.forEach(e -> {
                MappingStruct p = getMapFromResult(result, name);
                if (p == null) {
                    throw new OperationFailureException(operr("mappingName must be found in result, or it is invalid cfn json."));
                }
                p.getValues().put(e.getKey(), decodeValues(e.getValue(), result));
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
                MappingStruct p = null;
                for (MappingStruct m: result.getMappings()) {
                    if (m.getMappingName().equals(e.getKey())) {
                        p = m;
                        break;
                    }
                }
                if (p == null) {
                    p = new MappingStruct();
                    p.setMappingName(e.getKey());
                    result.getMappings().add(p);
                }
                decodeMappings(e.getValue(), result, e.getKey());
            });
        } else {
            throw new OperationFailureException(operr("Mappings root body must be json object!"));
        }
    }
}
