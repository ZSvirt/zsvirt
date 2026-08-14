package org.zstack.cloudformation.template.decoder;

import com.google.gson.JsonElement;
import org.zstack.cloudformation.template.function.TemplateFunctionUtils;
import org.zstack.cloudformation.template.struct.CfnResults;
import org.zstack.cloudformation.template.struct.CloudFormationConstants;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.Map;
import java.util.Set;

import static org.zstack.core.Platform.operr;

/**
 * Created by mingjian.deng on 2018/5/28.
 */
public class ConditionDecoder extends AbstractCfnRootDecoder {
    private static final CLogger logger = Utils.getLogger(ConditionDecoder.class);

    private Object decodeCondition(String key, JsonElement element, CfnResults result) {
        if (element.isJsonPrimitive()) {
            return element.getAsBoolean();
        } else if (element.isJsonObject()) {
            logger.debug(JSONObjectUtil.toJsonString(element.getAsJsonObject()));
            Set<Map.Entry<String, JsonElement>> es = element.getAsJsonObject().entrySet();
            if (es.size() > 1 || es.size() == 0) {
                throw new OperationFailureException(operr("" +
                        "Condition key: %s only support 1 element in the json object of value, but got %d elements!", key, es.size()));
            } else {
                Map.Entry<String, JsonElement> e = es.iterator().next();
                if (CloudFormationConstants.functions.contains(e.getKey())) {
                    Object o = TemplateFunctionUtils.getFunctions(e.getKey()).getFunctionResult(e.getValue(), result);
                    if (!(o instanceof Boolean)) {
                        throw new OperationFailureException(operr("Value must be boolean in 'Condition' field"));
                    }
                    return o;
                } else {
                    throw new OperationFailureException(operr("Only support ZStack Template Functions in 'Condition' field!"));
                }
            }
        } else {
            throw new OperationFailureException(operr("Condition body cannot support json null or array!"));
        }
    }

    @Override
    public void decode(JsonElement element, CfnResults result) {
        if (element.isJsonObject()) {
            Set<Map.Entry<String, JsonElement>> es = element.getAsJsonObject().entrySet();
            es.forEach(e -> {
                Object o = decodeCondition(e.getKey(), e.getValue(), result);
                result.getConditionParams().put(e.getKey(), Boolean.parseBoolean(o.toString()));
            });
            logger.debug(String.format("after decode conditions: %s", result.print()));
        } else {
            throw new OperationFailureException(operr("Mappings root body must be json object!"));
        }
    }
}
