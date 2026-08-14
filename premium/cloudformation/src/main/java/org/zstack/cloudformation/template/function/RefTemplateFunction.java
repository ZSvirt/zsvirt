package org.zstack.cloudformation.template.function;

import com.google.gson.JsonElement;
import org.zstack.cloudformation.template.struct.CfnResults;
import org.zstack.cloudformation.template.struct.ParameterStruct;
import org.zstack.cloudformation.template.struct.ResourceStruct;
import static org.zstack.core.Platform.operr;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.utils.CollectionDSL;

import java.util.List;

/**
 * Created by mingjian.deng on 2018/5/30.
 */
public class RefTemplateFunction implements TemplateFunction {
    public RefTemplateFunction() {
        super();
    }

    @Override
    public Object getFunctionResult(JsonElement element, CfnResults result) {
        if (element.isJsonPrimitive()) {
            String p = element.getAsString();
            // system default function
            if (p.startsWith("ZStack::") || p.startsWith("Cloud::")) {
                return "${" + p + "}";
            }

            // search ref from parameters
            for (ParameterStruct param: result.getParams()) {
                if (param.getParamName().equals(p)) {
                    Object value = param.getValue() == null ? param.getDefaultValue() : param.getValue();
                    if (value == null) {
                        throw new OperationFailureException(operr(String.format("Param [%s] has no value or default value found!", p)));
                    }
                    return value;
                }
            }
            // search ref from resources
            for (ResourceStruct resource: result.getResources()) {
                if (resource.getResourceName().equals(p)) {
                    return p;
                }
            }
            throw new OperationFailureException(operr(String.format("No Param [%s] found!", p)));
        } else {
            throw new OperationFailureException(operr(getErrorInfo()));
        }
    }

    @Override
    public List<String> getRefValue(JsonElement element, CfnResults result) {
        if (element.isJsonPrimitive()) {
            String p = element.getAsString();
            for (ResourceStruct resource: result.getResources()) {
                if (resource.getResourceName().equals(p)) {
                    return CollectionDSL.list(p);
                }
            }
        } else {
            throw new OperationFailureException(operr(getErrorInfo()));
        }
        return null;
    }

    @Override
    public String getErrorInfo() {
        return "Ref function only support String value!";
    }
}
