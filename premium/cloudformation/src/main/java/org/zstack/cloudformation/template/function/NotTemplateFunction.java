package org.zstack.cloudformation.template.function;

import com.google.gson.JsonElement;
import org.zstack.cloudformation.template.struct.CfnResults;
import org.zstack.header.errorcode.OperationFailureException;

import java.util.List;

import static org.zstack.core.Platform.operr;

/**
 * Created by mingjian.deng on 2019/9/27.
 */
public class NotTemplateFunction implements TemplateFunction {
    @Override
    public Object getFunctionResult(JsonElement element, CfnResults result) {
        if (element.isJsonObject()) {
            Object o = TemplateFunctionUtils.getObject(element, result);
            if (!(o instanceof Boolean)) {
                throw new OperationFailureException(operr(getErrorInfo()));
            }
            return !((Boolean)o);
        } else if (element.isJsonPrimitive()) {
            return !element.getAsBoolean();
        } else {
            throw new OperationFailureException(operr(getErrorInfo()));
        }
    }

    @Override
    public List<String> getRefValue(JsonElement element, CfnResults result) {
        if (element.isJsonObject()) {
            return TemplateFunctionUtils.getRef(element, result);
        }
        return null;
    }

    @Override
    public String getErrorInfo() {
        return "Fn::Not must be a JsonObject or a Boolean!";
    }
}
