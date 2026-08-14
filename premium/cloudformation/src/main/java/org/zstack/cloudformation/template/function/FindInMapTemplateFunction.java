package org.zstack.cloudformation.template.function;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.zstack.cloudformation.template.struct.CfnResults;
import org.zstack.cloudformation.template.struct.MappingStruct;
import org.zstack.header.errorcode.OperationFailureException;

import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;

/**
 * Created by mingjian.deng on 2018/6/4.
 */
public class FindInMapTemplateFunction implements TemplateFunction {
    @Override
    public Object getFunctionResult(JsonElement element, CfnResults result) {
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            checkElement(array);

            String p1 = array.get(0).toString();
            String p2 = array.get(1).getAsString();
            String p3 = null;
            if (array.size() == 3) {
                p3 = array.get(2).getAsString();
            }
            if (array.get(0).isJsonObject()) {
                p1 = TemplateFunctionUtils.getString(array.get(0), result, false);
            } else if (array.get(0).isJsonPrimitive()){
                p1 = TemplateFunctionUtils.getString(array.get(0).getAsString(), result);
            }
            for (MappingStruct m: result.getMappings()) {
                if (m.getMappingName().equals(p1)) {
                    Object o = m.getValues().get(p2);
                    if (p3 == null) {
                        return o;
                    } else {
                        return ((Map)o).get(p3);
                    }
                }
            }
            throw new OperationFailureException(operr(String.format("Fn::FindInMap cannot find resources [%s] in Resources!", array.get(0))));
        } else {
            throw new OperationFailureException(operr(getErrorInfo()));
        }
    }

    private void checkElement(JsonArray array) {
        if (array == null || (array.size() != 2 && array.size() !=3)) {
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
            }
        }
        return null;
    }

    @Override
    public String getErrorInfo() {
        return "Fn::FindInMap must be Array and contain 2 or 3 params!";
    }
}
