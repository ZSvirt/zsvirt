package org.zstack.cloudformation.template.function;

import com.google.gson.JsonElement;
import org.apache.commons.codec.binary.Base64;
import org.zstack.cloudformation.template.struct.CfnResults;
import org.zstack.header.errorcode.OperationFailureException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.zstack.core.Platform.operr;

/**
 * Created by mingjian.deng on 2018/6/4.
 */
public class Base64TemplateFunction implements TemplateFunction {
    @Override
    public Object getFunctionResult(JsonElement element, CfnResults result) {
        if (element.isJsonObject()) {
            Set<Map.Entry<String, JsonElement>> es = element.getAsJsonObject().entrySet();
            Object o = null;
            checkElement(es);
            for (Map.Entry<String, JsonElement> e: es) {
                o = TemplateFunctionUtils.getFunctions(e.getKey()).getFunctionResult(e.getValue(), result);
                if (!(o instanceof String)) {
                    throw new OperationFailureException(operr(String.format("%s cannot convert to String", e.getValue())));
                }
            }
            return Base64.encodeBase64String(String.valueOf(o).getBytes());
        } else if (element.isJsonPrimitive()) {
            return Base64.encodeBase64String(element.getAsString().getBytes());
        } else {
            throw new OperationFailureException(operr(getErrorInfo()));
        }
    }

    private void checkElement(Set<Map.Entry<String, JsonElement>> elements) {
        if (elements.size() > 1) {
            throw new OperationFailureException(operr(getErrorInfo()));
        }
    }

    @Override
    public List<String> getRefValue(JsonElement element, CfnResults result) {
        List<String> refs = new ArrayList<>();
        if (element.isJsonObject()) {
            Set<Map.Entry<String, JsonElement>> es = element.getAsJsonObject().entrySet();
            checkElement(es);
            es.forEach(e -> {
                if (e.getValue().isJsonArray()) {
                    for (JsonElement t: e.getValue().getAsJsonArray()) {
                        List<String> tmp = getRefValue(t, result);
                        if (tmp != null) {
                            refs.addAll(tmp);
                        }
                    }
                } else {
                    List<String> ref = TemplateFunctionUtils.getRef(e.getValue(), result);
                    if (ref != null) {
                        refs.addAll(ref);
                    }
                }
            });
        }
        if (refs.isEmpty()) {
            return null;
        }
        return refs;
    }

    @Override
    public String getErrorInfo() {
        return "Fn::Base64 function only support String or 1 Json elements!";
    }
}
